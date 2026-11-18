package com.wq.server.enums;

/**
 * Lifecycle of a tenant subscription.
 */
public enum SubscriptionStatus {

    /**
     * Checkout started but the subscription is not active yet.
     */
    INCOMPLETE,

    /**
     * Subscription is paid and the tenant entitlement is granted.
     */
    ACTIVE,

    /**
     * The latest invoice failed and the subscription is waiting for a retry.
     */
    PAST_DUE,

    /**
     * Subscription was canceled in Stripe.
     */
    CANCELED
}
