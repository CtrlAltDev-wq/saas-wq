package com.wq.framework.security;

import java.time.Instant;

/**
 * Validated token content required by the request pipeline.
 *
 * @param principal authenticated principal
 * @param tokenId   unique token identifier used for revocation
 * @param expiresAt absolute expiration instant
 */
public record JwtPayload(LoginUser principal, String tokenId, Instant expiresAt) {
}
