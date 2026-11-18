package com.wq.server.enums;

/**
 * Lifecycle of a payment order.
 */
public enum PaymentStatus {

    /**
     * Checkout session created, payment not confirmed yet.
     */
    PENDING,

    /**
     * Payment confirmed by a Stripe webhook.
     */
    PAID,

    /**
     * Checkout expired or the payment attempt failed.
     */
    FAILED
}
