package com.wq.server.vo;

import com.wq.server.entity.PaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Order representation returned to API consumers.
 *
 * @param id                      order identifier
 * @param orderNo                 human readable order number
 * @param customerId              billing customer identifier
 * @param planCode                purchased plan code
 * @param amountCents             amount in the smallest currency unit
 * @param currency                ISO 4217 currency code
 * @param status                  PENDING, PAID or FAILED
 * @param stripeCheckoutSessionId Stripe Checkout session identifier
 * @param stripeSubscriptionId    Stripe subscription identifier
 * @param paidAt                  payment confirmation time
 * @param createdAt               creation time
 */
@Schema(description = "Order")
public record OrderVO(Long id, String orderNo, Long customerId, String planCode, Long amountCents, String currency,
                      String status, String stripeCheckoutSessionId, String stripeSubscriptionId,
                      LocalDateTime paidAt, LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to its API representation.
     *
     * @param order entity
     * @return order view object
     */
    public static OrderVO from(PaymentOrder order) {
        return new OrderVO(order.getId(), order.getOrderNo(), order.getCustomerId(), order.getPlanCode(),
                order.getAmountCents(), order.getCurrency(), order.getStatus(), order.getStripeCheckoutSessionId(),
                order.getStripeSubscriptionId(), order.getPaidAt(), order.getCreatedAt());
    }
}
