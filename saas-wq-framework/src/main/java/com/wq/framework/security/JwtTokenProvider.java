package com.wq.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Creates and validates HMAC signed access tokens.
 *
 * <p>Tokens are stateless: the tenant, user and role are embedded as claims, so the
 * request pipeline can rebuild the security context without hitting the database.</p>
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TENANT_ID = "tenantId";

    private static final String CLAIM_USERNAME = "username";

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;

    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a signed access token for the given principal.
     *
     * @param loginUser authenticated principal
     * @return compact JWT string
     */
    public String generateToken(LoginUser loginUser) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .claim(CLAIM_TENANT_ID, loginUser.tenantId())
                .claim(CLAIM_USERNAME, loginUser.username())
                .claim(CLAIM_ROLE, loginUser.role())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature, issuer and expiration, then extracts the payload.
     *
     * @param token compact JWT string
     * @return validated payload
     * @throws io.jsonwebtoken.JwtException when the token is invalid or expired
     */
    public JwtPayload parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number tenantId = claims.get(CLAIM_TENANT_ID, Number.class);
        if (tenantId == null) {
            throw new MalformedJwtException("Access token does not contain a tenant identifier");
        }
        LoginUser principal = new LoginUser(
                Long.valueOf(claims.getSubject()),
                tenantId.longValue(),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE, String.class));
        return new JwtPayload(principal, claims.getId(), claims.getExpiration().toInstant());
    }

    /**
     * Exposes the configured token lifetime.
     *
     * @return token lifetime
     */
    public Duration getExpiration() {
        return properties.expiration();
    }
}
