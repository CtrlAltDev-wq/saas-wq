package com.wq.server.vo;

import com.wq.server.entity.TenantSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Subscription representation returned to API consumers.
 *
 * @param id                   subscription identifier
 * @param planCode             subscribed plan code
 * @param status               INCOMPLETE, ACTIVE, PAST_DUE or CANCELED
 * @param stripeSubscriptionId Stripe subscription identifier
 * @param currentPeriodStart   current billing period start
 * @param currentPeriodEnd     current billing period end
 * @param createdAt            creation time
 */
@Schema(description = "Tenant subscription")
public record SubscriptionVO(Long id, String planCode, String status, String stripeSubscriptionId,
                             LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd,
                             LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to its API representation.
     *
     * @param subscription entity
     * @return subscription view object
     */
    public static SubscriptionVO from(TenantSubscription subscription) {
        return new SubscriptionVO(subscription.getId(), subscription.getPlanCode(), subscription.getStatus(),
                subscription.getStripeSubscriptionId(), subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(), subscription.getCreatedAt());
    }
}
