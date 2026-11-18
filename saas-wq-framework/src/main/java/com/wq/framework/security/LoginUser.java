package com.wq.framework.security;

import com.wq.framework.tenant.TenantConstants;

/**
 * Authenticated principal resolved from a validated access token.
 *
 * @param userId   user identifier
 * @param tenantId owning tenant identifier
 * @param username login name
 * @param role     role name without the {@code ROLE_} prefix
 */
public record LoginUser(Long userId, Long tenantId, String username, String role) {

    /**
     * Indicates whether the principal belongs to the platform operator tenant.
     *
     * @return {@code true} for platform level users
     */
    public boolean isPlatformUser() {
        return tenantId != null && tenantId == TenantConstants.PLATFORM_TENANT_ID;
    }
}
