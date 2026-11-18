package com.wq.framework.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wq.common.api.Result;
import com.wq.common.api.ResultCode;
import com.wq.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces the per tenant quota before a controller is invoked.
 *
 * <p>Anonymous endpoints (login, Stripe webhook) carry no tenant and are therefore
 * passed through. Rejections are written directly to the response because an
 * interceptor runs outside the {@code @ControllerAdvice} scope.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TenantRateLimitService rateLimitService;

    private final RateLimitProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        if (!properties.enabled()) {
            return true;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true;
        }
        RateLimitResult result = rateLimitService.acquire(tenantId);
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAt()));
        if (result.allowed()) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfter()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.failure(ResultCode.RATE_LIMIT_EXCEEDED,
                "Quota of " + result.limit() + " requests per minute exceeded");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
