package com.wq.framework.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalised JWT settings bound from {@code wq.security.jwt.*}.
 *
 * @param secret     HMAC signing secret, at least 32 bytes for HS256
 * @param expiration token lifetime
 * @param issuer     issuer claim written into and required from every token
 */
@Validated
@ConfigurationProperties(prefix = "wq.security.jwt")
public record JwtProperties(@NotBlank String secret, @NotNull Duration expiration, @NotBlank String issuer) {
}
