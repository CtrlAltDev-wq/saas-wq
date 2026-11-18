package com.wq.server.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Checkout session returned to the frontend, which redirects the browser to {@code checkoutUrl}.
 *
 * @param orderId   local order identifier
 * @param orderNo   local order number
 * @param sessionId Stripe Checkout session identifier
 * @param checkoutUrl hosted Stripe Checkout page
 * @param amountCents amount in the smallest currency unit
 * @param currency  ISO 4217 currency code
 */
@Schema(description = "Stripe Checkout session")
public record CheckoutSessionVO(Long orderId, String orderNo, String sessionId, String checkoutUrl,
                                Long amountCents, String currency) {
}
