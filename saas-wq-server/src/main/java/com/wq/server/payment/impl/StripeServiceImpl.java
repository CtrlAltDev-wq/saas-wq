package com.wq.server.payment.impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.entity.Customer;
import com.wq.server.entity.PaymentOrder;
import com.wq.server.enums.PaymentStatus;
import com.wq.server.mapper.CustomerMapper;
import com.wq.server.mapper.PaymentOrderMapper;
import com.wq.server.payment.StripeProperties;
import com.wq.server.payment.StripeService;
import com.wq.server.vo.CheckoutSessionVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Stripe Checkout implementation. A pending order is persisted before the remote call so
 * that every checkout session can be reconciled by the webhook handler.
 */
@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeServiceImpl.class);

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final StripeProperties properties;
    private final CustomerMapper customerMapper;
    private final PaymentOrderMapper orderMapper;

    @Override
    public CheckoutSessionVO createCheckoutSession(Long customerId, String planCode) {
        String normalizedPlanCode = planCode.trim().toUpperCase(Locale.ROOT);
        StripeProperties.Plan plan = resolvePlan(normalizedPlanCode);
        requireConfiguration();

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Customer not found in the current tenant");
        }

        long tenantId = TenantContext.getRequiredTenantId();
        StripeClient client = new StripeClient(properties.getSecretKey());
        String stripeCustomerId = resolveStripeCustomer(client, customer, tenantId);

        PaymentOrder order = buildPendingOrder(tenantId, customerId, normalizedPlanCode);
        orderMapper.insert(order);

        try {
            Session session = client.checkout().sessions().create(buildSessionParams(order, plan, stripeCustomerId));
            order.setStripeCheckoutSessionId(session.getId());
            orderMapper.updateById(order);
            log.info("Created Stripe checkout session {} for order {}", session.getId(), order.getOrderNo());
            return new CheckoutSessionVO(order.getId(), order.getOrderNo(), session.getId(), session.getUrl(),
                    order.getAmountCents(), order.getCurrency());
        } catch (StripeException exception) {
            log.error("Stripe checkout creation failed for order {}", order.getOrderNo(), exception);
            order.setStatus(PaymentStatus.FAILED.name());
            orderMapper.updateById(order);
            throw new BusinessException(ResultCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    /**
     * Ensures the local customer is linked to a Stripe customer.
     *
     * @param client         Stripe client
     * @param customer       local customer
     * @param tenantId       owning tenant
     * @return Stripe customer identifier
     */
    private String resolveStripeCustomer(StripeClient client, Customer customer, long tenantId) {
        if (StringUtils.hasText(customer.getStripeCustomerId())) {
            return customer.getStripeCustomerId();
        }
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(customer.getName())
                .setEmail(customer.getEmail())
                .putMetadata("tenantId", String.valueOf(tenantId))
                .putMetadata("customerId", String.valueOf(customer.getId()))
                .build();
        try {
            com.stripe.model.Customer stripeCustomer = client.customers().create(params);
            customer.setStripeCustomerId(stripeCustomer.getId());
            customerMapper.updateById(customer);
            return stripeCustomer.getId();
        } catch (StripeException exception) {
            log.error("Stripe customer creation failed for customer {}", customer.getId(), exception);
            throw new BusinessException(ResultCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    /**
     * Builds the local pending order before the Stripe call.
     *
     * @param tenantId   owning tenant
     * @param customerId billing customer
     * @param planCode   normalized plan code
     * @return pending order
     */
    private PaymentOrder buildPendingOrder(long tenantId, Long customerId, String planCode) {
        StripeProperties.Plan plan = resolvePlan(planCode);
        PaymentOrder order = new PaymentOrder();
        order.setTenantId(tenantId);
        order.setCustomerId(customerId);
        order.setOrderNo(generateOrderNo());
        order.setPlanCode(planCode);
        order.setAmountCents(plan.getAmountCents());
        order.setCurrency(properties.getCurrency().toLowerCase(Locale.ROOT));
        order.setStatus(PaymentStatus.PENDING.name());
        return order;
    }

    /**
     * Builds the Stripe Checkout session parameters.
     *
     * @param order            local order
     * @param plan             plan definition
     * @param stripeCustomerId Stripe customer identifier
     * @return session parameters
     */
    private SessionCreateParams buildSessionParams(PaymentOrder order, StripeProperties.Plan plan,
                                                   String stripeCustomerId) {
        Map<String, String> metadata = Map.of(
                "orderId", String.valueOf(order.getId()),
                "tenantId", String.valueOf(order.getTenantId()),
                "planCode", order.getPlanCode());
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomerId)
                .setClientReferenceId(String.valueOf(order.getId()))
                .setSuccessUrl(properties.getSuccessUrl())
                .setCancelUrl(properties.getCancelUrl())
                .putAllMetadata(metadata)
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putAllMetadata(metadata)
                        .build());
        if (StringUtils.hasText(plan.getStripePriceId())) {
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(plan.getStripePriceId())
                    .setQuantity(1L)
                    .build());
        } else {
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency())
                            .setUnitAmount(order.getAmountCents())
                            .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                    .setInterval(resolveInterval(plan.getInterval()))
                                    .build())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(resolvePlanName(plan, order.getPlanCode()))
                                    .build())
                            .build())
                    .build());
        }
        return builder.build();
    }

    /**
     * Resolves a configured plan or fails with a business error.
     *
     * @param planCode normalized plan code
     * @return plan definition
     */
    private StripeProperties.Plan resolvePlan(String planCode) {
        StripeProperties.Plan plan = properties.getPlans().get(planCode);
        if (plan == null || plan.getAmountCents() == null || plan.getAmountCents() <= 0) {
            throw new BusinessException(ResultCode.PLAN_NOT_AVAILABLE, "Plan is not available: " + planCode);
        }
        return plan;
    }

    /**
     * Fails fast when the payment provider is not configured.
     */
    private void requireConfiguration() {
        if (!StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getSuccessUrl())
                || !StringUtils.hasText(properties.getCancelUrl())) {
            throw new BusinessException(ResultCode.PAYMENT_NOT_CONFIGURED);
        }
    }

    /**
     * Maps a configured interval to the Stripe enum.
     *
     * @param interval month or year
     * @return Stripe interval
     */
    private SessionCreateParams.LineItem.PriceData.Recurring.Interval resolveInterval(String interval) {
        return "year".equalsIgnoreCase(interval)
                ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH;
    }

    /**
     * Resolves the display name of a plan.
     *
     * @param plan     plan definition
     * @param planCode plan code fallback
     * @return display name
     */
    private String resolvePlanName(StripeProperties.Plan plan, String planCode) {
        return StringUtils.hasText(plan.getName()) ? plan.getName() : planCode;
    }

    /**
     * Generates a unique, sortable order number.
     *
     * @return order number
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }
}
