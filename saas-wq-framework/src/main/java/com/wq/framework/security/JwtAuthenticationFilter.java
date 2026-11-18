package com.wq.framework.security;

import com.wq.framework.logging.MdcContextFilter;
import com.wq.framework.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a {@code Bearer} access token.
 *
 * <p>Responsibilities: validate the token, reject revoked tokens, rebuild the security
 * context, bind the tenant for the rest of the request and always release it afterwards.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Optional permission resolver. When absent the principal only carries its role, which
     * keeps the filter usable in isolated tests and in modules without a role registry.
     */
    private final PermissionLookup permissionLookup;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenBlacklistService tokenBlacklistService,
                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                   PermissionLookup permissionLookup) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.permissionLookup = permissionLookup;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                JwtPayload payload = jwtTokenProvider.parseToken(token);
                if (tokenBlacklistService.isBlacklisted(payload.tokenId())) {
                    SecurityContextHolder.clearContext();
                    TenantContext.clear();
                    authenticationEntryPoint.commence(request, response,
                            new BadCredentialsException("Access token has been revoked"));
                    return;
                }
                authenticate(request, payload);
                TenantContext.set(payload.principal().tenantId());
                MDC.put(MdcContextFilter.TENANT_ID, String.valueOf(payload.principal().tenantId()));
                MDC.put(MdcContextFilter.USER_ID, String.valueOf(payload.principal().userId()));
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                TenantContext.clear();
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException("Access token is invalid or expired"));
                return;
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request, JwtPayload payload) {
        LoginUser principal = payload.principal();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.role()));
        if (permissionLookup != null) {
            permissionLookup.permissionsFor(principal.role())
                    .forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                payload.tokenId(),
                authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
