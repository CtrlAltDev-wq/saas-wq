package com.wq.server.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.entity.Customer;
import com.wq.server.entity.PaymentOrder;
import com.wq.server.entity.Tenant;
import com.wq.server.entity.TenantSubscription;
import com.wq.server.enums.PaymentStatus;
import com.wq.server.enums.SubscriptionStatus;
import com.wq.server.enums.TenantStatus;
import com.wq.server.mapper.CustomerMapper;
import com.wq.server.mapper.PaymentOrderMapper;
import com.wq.server.mapper.TenantMapper;
import com.wq.server.mapper.TenantSubscriptionMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Applies Stripe asynchronous events to local orders, subscriptions and tenant
 * entitlements. Every write is guarded by the current status so repeated deliveries stay
 * idempotent even without the Redis claim.
 */
@Component
@RequiredArgsConstructor
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String CHECKOUT_SESSION_EXPIRED = "checkout.session.expired";
    private static final String CHECKOUT_SESSION_ASYNC_FAILED = "checkout.session.async_payment_failed";
    private static final String INVOICE_PAYMENT_SUCCEEDED = "invoice.payment_succeeded";
    private static final String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    private static final String SUBSCRIPTION_DELETED = "customer.subscription.deleted";

    private final PaymentOrderMapper orderMapper;
    private final TenantSubscriptionMapper subscriptionMapper;
    private final CustomerMapper customerMapper;
    private final TenantMapper tenantMapper;
    private final StripeProperties properties;

    /**
     * Dispatches a verified Stripe event.
     *
     * @param event verified Stripe event
     */
    @Transactional
    public void handle(Event event) {
        StripeObject data = resolveData(event);
        if (data == null) {
            log.warn("Stripe event {} has no deserializable payload", event.getId());
            return;
        }
        switch (event.getType()) {
            case CHECKOUT_SESSION_COMPLETED -> {
                if (data instanceof Session session) {
                    handleCheckoutCompleted(session);
                }
            }
            case CHECKOUT_SESSION_EXPIRED, CHECKOUT_SESSION_ASYNC_FAILED -> {
                if (data instanceof Session session) {
                    markOrderFailed(session);
                }
            }
            case INVOICE_PAYMENT_SUCCEEDED -> {
                if (data instanceof Invoice invoice) {
                    handleInvoicePaid(invoice);
                }
            }
            case INVOICE_PAYMENT_FAILED -> {
                if (data instanceof Invoice invoice) {
                    handleInvoiceFailed(invoice);
                }
            }
            case SUBSCRIPTION_DELETED -> {
                if (data instanceof com.stripe.model.Subscription subscription) {
                    handleSubscriptionDeleted(subscription);
                }
            }
            default -> log.debug("Ignored Stripe event type {}", event.getType());
        }
    }

    /**
     * Deserializes the event payload. Stripe only returns the typed object when the event
     * API version matches the SDK version, so older or newer versions fall back to an
     * unchecked deserialization of the same JSON document.
     *
     * @param event verified Stripe event
     * @return typed payload or {@code null}
     */
    private StripeObject resolveData(Event event) {
        StripeObject data = event.getDataObjectDeserializer().getObject().orElse(null);
        if (data != null) {
            return data;
        }
        try {
            log.warn("Stripe event {} uses API version {}, deserializing without version check",
                    event.getId(), event.getApiVersion());
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException exception) {
            log.error("Unable to deserialize the payload of Stripe event {}", event.getId(), exception);
            return null;
        }
    }

    /**
     * Confirms the payment, activates the subscription and grants the tenant plan.
     *
     * @param session completed Checkout session
     */
    private void handleCheckoutCompleted(Session session) {
        PaymentOrder order = findOrder(session);
        if (order == null) {
            log.warn("No local order found for Stripe checkout session {}", session.getId());
            return;
        }
        if (PaymentStatus.PAID.name().equals(order.getStatus())) {
            log.info("Order {} is already paid, skipping duplicate event", order.getOrderNo());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            TenantContext.set(order.getTenantId());
            order.setStatus(PaymentStatus.PAID.name());
            order.setPaidAt(now);
            order.setStripeSubscriptionId(session.getSubscription());
            orderMapper.updateById(order);

            LocalDateTime periodEnd = calculatePeriodEnd(order.getPlanCode(), now);
            upsertSubscription(order, session.getSubscription(), now, periodEnd);
            activateTenant(order.getTenantId(), order.getPlanCode(), periodEnd);
            bindStripeCustomer(order.getCustomerId(), session.getCustomer());
            log.info("Checkout completed: order {} paid, tenant {} granted plan {}",
                    order.getOrderNo(), order.getTenantId(), order.getPlanCode());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extends the subscription after a successful invoice payment.
     *
     * @param invoice paid invoice
     */
    private void handleInvoicePaid(Invoice invoice) {
        String stripeSubscriptionId = invoice.getSubscription();
        if (!StringUtils.hasText(stripeSubscriptionId)) {
            log.debug("Invoice {} is not linked to a subscription, ignored", invoice.getId());
            return;
        }
        TenantSubscription subscription = subscriptionMapper.selectBySubscriptionIdGlobally(stripeSubscriptionId);
        PaymentOrder order = orderMapper.selectBySubscriptionIdGlobally(stripeSubscriptionId);
        if (subscription == null && order == null) {
            log.warn("No local record found for Stripe subscription {}", stripeSubscriptionId);
            return;
        }
        long tenantId = subscription != null ? subscription.getTenantId() : order.getTenantId();
        String planCode = subscription != null ? subscription.getPlanCode() : order.getPlanCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime[] period = resolveInvoicePeriod(invoice, planCode, now);
        try {
            TenantContext.set(tenantId);
            if (subscription == null) {
                subscription = new TenantSubscription();
                subscription.setTenantId(tenantId);
                subscription.setCustomerId(order.getCustomerId());
                subscription.setPlanCode(planCode);
                subscription.setStatus(SubscriptionStatus.ACTIVE.name());
                subscription.setStripeSubscriptionId(stripeSubscriptionId);
                subscription.setCurrentPeriodStart(period[0]);
                subscription.setCurrentPeriodEnd(period[1]);
                subscriptionMapper.insert(subscription);
            } else {
                subscription.setStatus(SubscriptionStatus.ACTIVE.name());
                subscription.setCurrentPeriodStart(period[0]);
                subscription.setCurrentPeriodEnd(period[1]);
                subscriptionMapper.updateById(subscription);
            }
            if (order != null && !PaymentStatus.PAID.name().equals(order.getStatus())) {
                order.setStatus(PaymentStatus.PAID.name());
                order.setPaidAt(now);
                orderMapper.updateById(order);
            }
            activateTenant(tenantId, planCode, period[1]);
            log.info("Invoice {} paid, subscription {} renewed until {}",
                    invoice.getId(), stripeSubscriptionId, period[1]);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Flags a subscription as past due after a failed invoice payment.
     *
     * @param invoice failed invoice
     */
    private void handleInvoiceFailed(Invoice invoice) {
        String stripeSubscriptionId = invoice.getSubscription();
        if (!StringUtils.hasText(stripeSubscriptionId)) {
            return;
        }
        TenantSubscription subscription = subscriptionMapper.selectBySubscriptionIdGlobally(stripeSubscriptionId);
        if (subscription == null) {
            log.warn("No local subscription found for Stripe subscription {}", stripeSubscriptionId);
            return;
        }
        try {
            TenantContext.set(subscription.getTenantId());
            subscription.setStatus(SubscriptionStatus.PAST_DUE.name());
            subscriptionMapper.updateById(subscription);
            log.warn("Invoice payment failed for subscription {}, marked as past due", stripeSubscriptionId);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Marks the subscription as canceled, the tenant keeps its entitlement until expiry.
     *
     * @param stripeSubscription canceled Stripe subscription
     */
    private void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        TenantSubscription subscription =
                subscriptionMapper.selectBySubscriptionIdGlobally(stripeSubscription.getId());
        if (subscription == null) {
            log.warn("No local subscription found for Stripe subscription {}", stripeSubscription.getId());
            return;
        }
        try {
            TenantContext.set(subscription.getTenantId());
            subscription.setStatus(SubscriptionStatus.CANCELED.name());
            subscription.setCanceledAt(LocalDateTime.now());
            subscriptionMapper.updateById(subscription);
            log.info("Subscription {} canceled, entitlement is kept until {}",
                    stripeSubscription.getId(), subscription.getCurrentPeriodEnd());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Marks a still pending order as failed, for expired or asynchronously failed checkouts.
     *
     * @param session Checkout session
     */
    private void markOrderFailed(Session session) {
        PaymentOrder order = findOrder(session);
        if (order == null || !PaymentStatus.PENDING.name().equals(order.getStatus())) {
            return;
        }
        try {
            TenantContext.set(order.getTenantId());
            order.setStatus(PaymentStatus.FAILED.name());
            orderMapper.updateById(order);
            log.info("Order {} marked as failed after Stripe event", order.getOrderNo());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Locates the local order of a Checkout session, falling back to the client reference.
     *
     * @param session Checkout session
     * @return order or {@code null}
     */
    private PaymentOrder findOrder(Session session) {
        PaymentOrder order = orderMapper.selectByCheckoutSessionIdGlobally(session.getId());
        if (order != null) {
            return order;
        }
        if (StringUtils.hasText(session.getClientReferenceId())) {
            try {
                return orderMapper.selectByIdGlobally(Long.valueOf(session.getClientReferenceId()));
            } catch (NumberFormatException exception) {
                log.warn("Invalid client reference {} on Stripe session {}",
                        session.getClientReferenceId(), session.getId());
            }
        }
        return null;
    }

    /**
     * Creates or refreshes the subscription of the tenant.
     *
     * @param order              paid order
     * @param stripeSubscription Stripe subscription identifier
     * @param periodStart        period start
     * @param periodEnd          period end
     */
    private void upsertSubscription(PaymentOrder order, String stripeSubscription,
                                    LocalDateTime periodStart, LocalDateTime periodEnd) {
        TenantSubscription subscription = subscriptionMapper.selectOne(
                Wrappers.<TenantSubscription>lambdaQuery()
                        .eq(TenantSubscription::getTenantId, order.getTenantId()));
        if (subscription == null) {
            subscription = new TenantSubscription();
            subscription.setTenantId(order.getTenantId());
            subscription.setCustomerId(order.getCustomerId());
            subscription.setPlanCode(order.getPlanCode());
            subscription.setStatus(SubscriptionStatus.ACTIVE.name());
            subscription.setStripeSubscriptionId(stripeSubscription);
            subscription.setCurrentPeriodStart(periodStart);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscriptionMapper.insert(subscription);
            return;
        }
        subscription.setCustomerId(order.getCustomerId());
        subscription.setPlanCode(order.getPlanCode());
        subscription.setStatus(SubscriptionStatus.ACTIVE.name());
        subscription.setStripeSubscriptionId(stripeSubscription);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscriptionMapper.updateById(subscription);
    }

    /**
     * Grants the plan to the tenant and keeps the tenant enabled.
     *
     * @param tenantId  tenant identifier
     * @param planCode  granted plan
     * @param expiresAt entitlement expiry
     */
    private void activateTenant(long tenantId, String planCode, LocalDateTime expiresAt) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            log.warn("Tenant {} no longer exists, entitlement skipped", tenantId);
            return;
        }
        tenant.setPlanCode(planCode);
        tenant.setPlanExpiresAt(expiresAt);
        tenant.setStatus(TenantStatus.ACTIVE.name());
        tenantMapper.updateById(tenant);
    }

    /**
     * Stores the Stripe customer identifier on the local customer when it is still empty.
     *
     * @param customerId       local customer identifier
     * @param stripeCustomerId Stripe customer identifier
     */
    private void bindStripeCustomer(Long customerId, String stripeCustomerId) {
        if (!StringUtils.hasText(stripeCustomerId)) {
            return;
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || StringUtils.hasText(customer.getStripeCustomerId())) {
            return;
        }
        customer.setStripeCustomerId(stripeCustomerId);
        customerMapper.updateById(customer);
    }

    /**
     * Resolves the billing period from the invoice lines, falling back to the plan interval.
     *
     * @param invoice  paid invoice
     * @param planCode plan code
     * @param fallback period start fallback
     * @return period start and end
     */
    private LocalDateTime[] resolveInvoicePeriod(Invoice invoice, String planCode, LocalDateTime fallback) {
        LocalDateTime start = fallback;
        LocalDateTime end = null;
        if (invoice.getLines() != null) {
            List<InvoiceLineItem> lines = invoice.getLines().getData();
            if (lines != null && !lines.isEmpty()) {
                InvoiceLineItem.Period period = lines.get(0).getPeriod();
                if (period != null) {
                    start = period.getStart() != null ? toLocalDateTime(period.getStart()) : start;
                    end = period.getEnd() != null ? toLocalDateTime(period.getEnd()) : null;
                }
            }
        }
        if (end == null) {
            end = calculatePeriodEnd(planCode, start);
        }
        return new LocalDateTime[]{start, end};
    }

    /**
     * Calculates an entitlement expiry from the configured billing interval.
     *
     * @param planCode plan code
     * @param start    period start
     * @return period end
     */
    private LocalDateTime calculatePeriodEnd(String planCode, LocalDateTime start) {
        StripeProperties.Plan plan = properties.getPlans().get(planCode);
        boolean yearly = plan != null && "year".equalsIgnoreCase(plan.getInterval());
        return yearly ? start.plusYears(1) : start.plusMonths(1);
    }

    /**
     * Converts an epoch second value to the system time zone.
     *
     * @param epochSecond epoch seconds
     * @return local date time
     */
    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }
}
