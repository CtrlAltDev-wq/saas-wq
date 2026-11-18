package com.wq.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wq.framework.security.JwtPayload;
import com.wq.framework.security.JwtProperties;
import com.wq.framework.security.JwtTokenProvider;
import com.wq.framework.security.LoginUser;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Token issuance and validation rules.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes";

    private final LoginUser loginUser = new LoginUser(7L, 2L, "acme.admin", "TENANT_ADMIN");

    @Test
    void issuesAndParsesRoundTripToken() {
        JwtTokenProvider provider = provider(Duration.ofHours(1), "saas-wq");

        JwtPayload payload = provider.parseToken(provider.generateToken(loginUser));

        assertEquals(7L, payload.principal().userId());
        assertEquals(2L, payload.principal().tenantId());
        assertEquals("acme.admin", payload.principal().username());
        assertEquals("TENANT_ADMIN", payload.principal().role());
        assertNotNull(payload.tokenId());
        assertTrue(payload.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = provider(Duration.ofHours(1), "saas-wq");
        String token = provider.generateToken(loginUser);

        assertThrows(JwtException.class, () -> provider.parseToken(token + "x"));
    }

    @Test
    void rejectsTokenSignedByAnotherIssuer() {
        JwtTokenProvider issuer = provider(Duration.ofHours(1), "another-issuer");
        String token = issuer.generateToken(loginUser);

        assertThrows(JwtException.class, () -> provider(Duration.ofHours(1), "saas-wq").parseToken(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = provider(Duration.ofSeconds(-10), "saas-wq");
        String token = provider.generateToken(loginUser);

        assertThrows(JwtException.class, () -> provider.parseToken(token));
    }

    private JwtTokenProvider provider(Duration expiration, String issuer) {
        return new JwtTokenProvider(new JwtProperties(SECRET, expiration, issuer));
    }
}
