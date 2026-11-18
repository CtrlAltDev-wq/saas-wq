package com.wq.framework.ratelimit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Externalised quota settings bound from {@code wq.rate-limit.*}.
 *
 * @param enabled           whether tenant based throttling is active
 * @param requestsPerMinute allowed requests inside one fixed one minute window
 * @param keyPrefix         Redis key namespace, changed per environment
 */
@Validated
@ConfigurationProperties(prefix = "wq.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60") @Min(1) int requestsPerMinute,
        @DefaultValue("saas-wq:rate-limit:") @NotBlank String keyPrefix) {
}
