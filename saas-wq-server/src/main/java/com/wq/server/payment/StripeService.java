package com.wq.server.payment;

import com.wq.server.vo.CheckoutSessionVO;

/**
 * Creates hosted Stripe Checkout sessions for tenant subscriptions.
 */
public interface StripeService {

    /**
     * Creates a pending local order and the matching Stripe Checkout subscription session.
     *
     * @param customerId billing customer identifier, must belong to the current tenant
     * @param planCode   configured plan code
     * @return checkout session details including the hosted payment page URL
     */
    CheckoutSessionVO createCheckoutSession(Long customerId, String planCode);
}
