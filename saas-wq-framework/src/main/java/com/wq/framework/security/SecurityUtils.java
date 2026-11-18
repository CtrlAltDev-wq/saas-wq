package com.wq.framework.security;

import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessors for the authenticated principal.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Returns the authenticated principal or fails with 401.
     *
     * @return authenticated principal
     */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * Returns the identifier of the authenticated user.
     *
     * @return user identifier
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().userId();
    }

    /**
     * Returns the tenant of the authenticated user.
     *
     * @return tenant identifier
     */
    public static Long getCurrentTenantId() {
        return getCurrentUser().tenantId();
    }
}
