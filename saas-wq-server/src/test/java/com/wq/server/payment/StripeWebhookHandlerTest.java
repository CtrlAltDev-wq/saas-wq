package com.wq.server.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import com.wq.server.entity.Customer;
import com.wq.server.entity.PaymentOrder;
import com.wq.server.entity.Tenant;
import com.wq.server.entity.TenantSubscription;
import com.wq.server.mapper.CustomerMapper;
import com.wq.server.mapper.PaymentOrderMapper;
import com.wq.server.mapper.TenantMapper;
import com.wq.server.mapper.TenantSubscriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Stripe webhook side effects on orders, subscriptions and tenant entitlement.
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookHandlerTest {

    @Mock
    private PaymentOrderMapper orderMapper;

    @Mock
    private TenantSubscriptionMapper subscriptionMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private TenantMapper tenantMapper;

    private StripeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StripeProperties();
        StripeProperties.Plan plan = new StripeProperties.Plan();
        plan.setName("Professional");
        plan.setInterval("month");
        properties.getPlans().put("PROFESSIONAL", plan);
    }

    @Test
    void checkoutCompletedPaysTheOrderAndGrantsThePlan() {
        PaymentOrder order = pendingOrder();
        Tenant tenant = tenant();
        Customer customer = customer();
        when(orderMapper.selectByCheckoutSessionIdGlobally("cs_test_1")).thenReturn(order);
        when(subscriptionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(tenantMapper.selectById(2L)).thenReturn(tenant);
        when(customerMapper.selectById(5L)).thenReturn(customer);

        handler().handle(event("checkout.session.completed", """
                {"id":"cs_test_1","object":"checkout.session","client_reference_id":"10",
                 "customer":"cus_test","subscription":"sub_test"}"""));

        assertEquals("PAID", order.getStatus());
        assertNotNull(order.getPaidAt());
        assertEquals("sub_test", order.getStripeSubscriptionId());
        assertEquals("PROFESSIONAL", tenant.getPlanCode());
        assertNotNull(tenant.getPlanExpiresAt());
        assertEquals("cus_test", customer.getStripeCustomerId());
        verify(orderMapper).updateById(order);
        verify(subscriptionMapper).insert(any(TenantSubscription.class));
    }

    @Test
    void repeatedCheckoutEventDoesNotChargeOrGrantTwice() {
        PaymentOrder order = pendingOrder();
        order.setStatus("PAID");
        when(orderMapper.selectByCheckoutSessionIdGlobally("cs_test_1")).thenReturn(order);

        handler().handle(event("checkout.session.completed", """
                {"id":"cs_test_1","object":"checkout.session","customer":"cus_test","subscription":"sub_test"}"""));

        verify(orderMapper, org.mockito.Mockito.never()).updateById(any(PaymentOrder.class));
        verifyNoInteractions(tenantMapper);
    }

    @Test
    void failedInvoiceMarksTheSubscriptionPastDue() {
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(3L);
        subscription.setTenantId(2L);
        subscription.setStatus("ACTIVE");
        when(subscriptionMapper.selectBySubscriptionIdGlobally("sub_test")).thenReturn(subscription);

        handler().handle(event("invoice.payment_failed", """
                {"id":"in_test_1","object":"invoice","subscription":"sub_test"}"""));

        assertEquals("PAST_DUE", subscription.getStatus());
        verify(subscriptionMapper).updateById(subscription);
    }

    @Test
    void unknownEventTypeIsIgnored() {
        handler().handle(event("customer.created", """
                {"id":"cus_test","object":"customer"}"""));

        verifyNoInteractions(orderMapper, subscriptionMapper, tenantMapper, customerMapper);
    }

    private StripeWebhookHandler handler() {
        return new StripeWebhookHandler(orderMapper, subscriptionMapper, customerMapper, tenantMapper, properties);
    }

    private PaymentOrder pendingOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(10L);
        order.setTenantId(2L);
        order.setCustomerId(5L);
        order.setOrderNo("ORD-20260101-0001");
        order.setPlanCode("PROFESSIONAL");
        order.setStatus("PENDING");
        return order;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(2L);
        tenant.setCode("acme");
        tenant.setStatus("ACTIVE");
        return tenant;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setId(5L);
        return customer;
    }

    private Event event(String type, String dataObject) {
        String json = """
                {"id":"evt_test","object":"event","api_version":"%s","type":"%s","data":{"object":%s}}"""
                .formatted(Stripe.API_VERSION, type, dataObject);
        return ApiResource.GSON.fromJson(json, Event.class);
    }
}
