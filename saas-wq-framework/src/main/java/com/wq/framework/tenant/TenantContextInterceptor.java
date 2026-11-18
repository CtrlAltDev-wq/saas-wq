package com.wq.framework.tenant;

import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards tenant scoped requests and releases the tenant binding after completion.
 *
 * <p>The interceptor is a safety net around the JWT filter: it rejects authenticated
 * requests that somehow reached the MVC layer without a tenant, and it always clears
 * {@link TenantContext} at the end of the request.</p>
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser && !TenantContext.isSet()) {
            throw new BusinessException(ResultCode.TENANT_REQUIRED);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception exception) {
        TenantContext.clear();
    }
}
