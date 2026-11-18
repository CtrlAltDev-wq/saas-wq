package com.wq.framework.ratelimit;

/**
 * Outcome of a single quota check.
 *
 * @param allowed         whether the request may proceed
 * @param limit           allowed requests per window
 * @param remaining       requests left in the current window
 * @param retryAfter      seconds until the window resets
 * @param resetAt         epoch second at which the window resets
 */
public record RateLimitResult(boolean allowed, int limit, long remaining, long retryAfter, long resetAt) {

    /**
     * Builds the result of a check.
     *
     * @param allowed   whether the request may proceed
     * @param limit     allowed requests per window
     * @param used      requests consumed in the window
     * @param retryAfter seconds until the window resets
     * @param resetAt   epoch second at which the window resets
     * @return quota outcome
     */
    public static RateLimitResult of(boolean allowed, int limit, long used, long retryAfter, long resetAt) {
        long remaining = Math.max(0L, limit - used);
        return new RateLimitResult(allowed, limit, remaining, retryAfter, resetAt);
    }

    /**
     * Builds a permissive result used when Redis is unavailable (fail open).
     *
     * @param limit   allowed requests per window
     * @param resetAt epoch second at which the window resets
     * @return quota outcome that lets the request through
     */
    public static RateLimitResult failOpen(int limit, long resetAt) {
        return new RateLimitResult(true, limit, limit, 0L, resetAt);
    }
}
