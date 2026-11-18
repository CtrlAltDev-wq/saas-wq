package com.wq.server.payment;

import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Redis backed idempotency guard for Stripe webhook deliveries. Stripe retries failed
 * deliveries, and may also deliver the same event more than once, so each event id is
 * claimed before the handler runs.
 */
@Component
@RequiredArgsConstructor
public class StripeEventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(StripeEventDeduplicator.class);

    private static final String KEY_PREFIX = "saas-wq:stripe:event:";

    private static final Duration RETENTION = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    /**
     * Claims an event id. Claiming fails closed: when Redis is unavailable the webhook is
     * rejected with a retryable error so that Stripe delivers it again later.
     *
     * @param eventId Stripe event identifier
     * @return true when the caller owns the event, false when it was already processed
     */
    public boolean register(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return true;
        }
        try {
            Boolean claimed = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + eventId, "1", RETENTION);
            return Boolean.TRUE.equals(claimed);
        } catch (DataAccessException exception) {
            log.error("Unable to claim Stripe event {}", eventId, exception);
            throw new BusinessException(ResultCode.IDEMPOTENCY_STORE_UNAVAILABLE);
        }
    }

    /**
     * Releases a claim after a failed handling attempt so the retry can be processed.
     *
     * @param eventId Stripe event identifier
     */
    public void release(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + eventId);
        } catch (DataAccessException exception) {
            log.warn("Unable to release Stripe event claim {}", eventId, exception);
        }
    }
}
