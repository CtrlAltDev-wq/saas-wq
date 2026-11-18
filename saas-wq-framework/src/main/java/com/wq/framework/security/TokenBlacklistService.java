package com.wq.framework.security;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis backed revocation list for access tokens.
 *
 * <p>JWT is stateless, so logout is implemented with a deny list keyed by the token
 * identifier and bounded by the remaining token lifetime. Redis failures degrade to
 * "token still valid" instead of breaking logout or every authenticated request.</p>
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "saas-wq:jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Marks a token as revoked until it would expire anyway.
     *
     * @param tokenId    unique token identifier
     * @param timeToLive remaining lifetime
     */
    public void blacklist(String tokenId, Duration timeToLive) {
        if (tokenId == null || timeToLive == null || timeToLive.isNegative() || timeToLive.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", timeToLive);
        } catch (DataAccessException exception) {
            log.warn("Unable to persist token revocation entry: {}", exception.getMessage());
        }
    }

    /**
     * Indicates whether a token has been revoked.
     *
     * @param tokenId unique token identifier
     * @return {@code true} when revoked
     */
    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
        } catch (DataAccessException exception) {
            log.warn("Unable to read token revocation list: {}", exception.getMessage());
            return false;
        }
    }
}
