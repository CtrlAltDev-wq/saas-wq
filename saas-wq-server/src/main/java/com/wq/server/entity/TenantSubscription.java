package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Current subscription of a tenant. Each tenant owns at most one active record.
 */
@Getter
@Setter
@TableName("biz_subscription")
public class TenantSubscription extends BaseEntity {

    /**
     * Owning tenant, enforced by the row level isolation plugin.
     */
    private Long tenantId;

    /**
     * Billing customer of the subscription.
     */
    private Long customerId;

    /**
     * Subscribed plan code.
     */
    private String planCode;

    /**
     * {@link com.wq.server.enums.SubscriptionStatus} value.
     */
    private String status;

    /**
     * Stripe subscription identifier.
     */
    private String stripeSubscriptionId;

    /**
     * Current billing period start.
     */
    private LocalDateTime currentPeriodStart;

    /**
     * Current billing period end, also used as the tenant entitlement expiry.
     */
    private LocalDateTime currentPeriodEnd;

    /**
     * Cancellation time.
     */
    private LocalDateTime canceledAt;
}
