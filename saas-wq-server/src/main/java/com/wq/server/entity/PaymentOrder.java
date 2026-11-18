package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Payment attempt created for a Stripe Checkout session.
 */
@Getter
@Setter
@TableName("biz_order")
public class PaymentOrder extends BaseEntity {

    /**
     * Owning tenant, enforced by the row level isolation plugin.
     */
    private Long tenantId;

    /**
     * Billing customer that owns this order.
     */
    private Long customerId;

    /**
     * Human readable order number.
     */
    private String orderNo;

    /**
     * Purchased plan code, for example STARTER.
     */
    private String planCode;

    /**
     * Amount in the smallest currency unit.
     */
    private Long amountCents;

    /**
     * ISO 4217 currency code.
     */
    private String currency;

    /**
     * {@link com.wq.server.enums.PaymentStatus} value.
     */
    private String status;

    /**
     * Stripe Checkout session identifier.
     */
    private String stripeCheckoutSessionId;

    /**
     * Stripe subscription identifier, filled once the checkout completes.
     */
    private String stripeSubscriptionId;

    /**
     * Payment confirmation time.
     */
    private LocalDateTime paidAt;
}
