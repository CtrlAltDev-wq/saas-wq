package com.wq.framework.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Tenant aware request throttling backed by Redis.
 *
 * <p>Each tenant owns a counter per fixed one minute window. The increment and the
 * expiration are performed by a single Lua script so that concurrent requests cannot
 * observe or produce a partially applied counter.</p>
 *
 * <p>Failures are fail open: a Redis outage must not take the whole API down, so the
 * request is allowed and the incident is logged.</p>
 */
@Slf4j
@Service
public class TenantRateLimitService {

    private static final long WINDOW_SECONDS = 60L;

    private static final long COUNTER_TTL_SECONDS = 120L;

    private final StringRedisTemplate redisTemplate;

    private final RateLimitProperties properties;

    private final DefaultRedisScript<Long> script;

    private final MeterRegistry meterRegistry;

    public TenantRateLimitService(StringRedisTemplate redisTemplate, RateLimitProperties properties,
                                  MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/tenant_rate_limit.lua"));
        redisScript.setResultType(Long.class);
        this.script = redisScript;
    }

    /**
     * Consumes one unit of the tenant quota.
     *
     * @param tenantId tenant identifier
     * @return quota outcome for this request
     */
    public RateLimitResult acquire(long tenantId) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - (now % WINDOW_SECONDS);
        long resetAt = windowStart + WINDOW_SECONDS;
        long retryAfter = Math.max(1L, resetAt - now);
        int limit = properties.requestsPerMinute();
        String key = properties.keyPrefix() + tenantId + ":" + windowStart;
        try {
            Long used = redisTemplate.execute(script, List.of(key), Long.toString(COUNTER_TTL_SECONDS));
            if (used == null) {
                log.warn("Rate limiter returned no counter for tenant {}, request allowed", tenantId);
                meterRegistry.counter("saaswq.rate.limit.failopen").increment();
                return RateLimitResult.failOpen(limit, resetAt);
            }
            return RateLimitResult.of(used <= limit, limit, used, retryAfter, resetAt);
        } catch (DataAccessException exception) {
            log.warn("Rate limiter unavailable for tenant {}, request allowed: {}", tenantId, exception.getMessage());
            meterRegistry.counter("saaswq.rate.limit.failopen").increment();
            return RateLimitResult.failOpen(limit, resetAt);
        }
    }
}
