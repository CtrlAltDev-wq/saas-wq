package com.wq.server.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.wq.framework.ratelimit.RateLimitProperties;
import com.wq.framework.ratelimit.RateLimitResult;
import com.wq.framework.ratelimit.TenantRateLimitService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Behaviour of the Redis backed tenant quota.
 */
@ExtendWith(MockitoExtension.class)
class TenantRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void allowsRequestWithinQuota() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(3L);

        RateLimitResult result = service(3).acquire(2L);

        assertTrue(result.allowed());
        assertEquals(0, result.remaining());
        assertEquals(3, result.limit());
    }

    @Test
    void rejectsRequestAboveQuota() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(4L);

        RateLimitResult result = service(3).acquire(2L);

        assertFalse(result.allowed());
        assertTrue(result.retryAfter() > 0);
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        RateLimitResult result = service(3).acquire(2L);

        assertTrue(result.allowed());
        assertEquals(1.0, meterRegistry.get("saaswq.rate.limit.failopen").counter().count());
    }

    @Test
    void failsOpenWhenTheScriptReturnsNoCounter() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(null);

        RateLimitResult result = service(3).acquire(2L);

        assertTrue(result.allowed());
        assertEquals(1.0, meterRegistry.get("saaswq.rate.limit.failopen").counter().count());
    }

    private TenantRateLimitService service(int requestsPerMinute) {
        return new TenantRateLimitService(redisTemplate,
                new RateLimitProperties(true, requestsPerMinute, "test:rate:"), meterRegistry);
    }
}
