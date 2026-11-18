package com.wq.framework.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guarantees that request scoped diagnostic fields never leak into the next request served
 * by the same pooled thread.
 *
 * <p>{@link com.wq.framework.security.JwtAuthenticationFilter} populates the tenant and
 * user identifiers once the token is validated; this filter is the outermost one and is
 * therefore responsible for the unconditional cleanup.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcContextFilter extends OncePerRequestFilter {

    /**
     * MDC key carrying the tenant identifier.
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * MDC key carrying the authenticated user identifier.
     */
    public static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TENANT_ID);
            MDC.remove(USER_ID);
        }
    }
}
