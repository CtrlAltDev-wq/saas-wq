package com.wq.server.enums;

/**
 * Roles recognised by the platform.
 */
public enum UserRole {

    /**
     * Operator of the SaaS platform itself, allowed to manage tenants.
     */
    SUPER_ADMIN,

    /**
     * Administrator of a single tenant.
     */
    TENANT_ADMIN,

    /**
     * Regular member of a tenant.
     */
    MEMBER
}
