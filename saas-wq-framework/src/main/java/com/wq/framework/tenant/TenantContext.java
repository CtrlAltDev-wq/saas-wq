package com.wq.framework.tenant;

import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;

/**
 * Thread bound holder of the tenant under which the current request executes.
 *
 * <p>The value is populated once the JWT is validated and cleared in a {@code finally}
 * block so that pooled request threads never leak a tenant into the next request.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Binds the tenant to the current thread.
     *
     * @param tenantId tenant identifier
     */
    public static void set(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Returns the bound tenant or {@code null} when none is bound.
     *
     * @return current tenant identifier
     */
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Indicates whether a tenant is bound to the current thread.
     *
     * @return {@code true} when bound
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Returns the bound tenant and fails fast when it is missing.
     *
     * <p>Fail fast is deliberate: silently falling back to an unfiltered query would
     * expose data across tenants.</p>
     *
     * @return current tenant identifier
     */
    public static Long getRequiredTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * Removes the tenant from the current thread.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
