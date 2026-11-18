package com.wq.server.enums;

/**
 * Lifecycle state of a tenant.
 */
public enum TenantStatus {

    /**
     * Tenant can authenticate and use the API.
     */
    ACTIVE,

    /**
     * Tenant is suspended; authentication is rejected.
     */
    DISABLED
}
