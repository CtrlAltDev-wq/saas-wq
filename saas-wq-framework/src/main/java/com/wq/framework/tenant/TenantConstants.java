package com.wq.framework.tenant;

import java.util.Locale;
import java.util.Set;

/**
 * Shared multi-tenant constants.
 */
public final class TenantConstants {

    /**
     * Identifier of the platform tenant that owns the SaaS operator itself.
     *
     * <p>It is a regular row of {@code sys_tenant}, which keeps referential integrity for
     * platform users while the tenant table itself stays outside row filtering.</p>
     */
    public static final long PLATFORM_TENANT_ID = 1L;

    /**
     * Physical column carrying the tenant discriminator.
     */
    public static final String TENANT_ID_COLUMN = "tenant_id";

    /**
     * Tables that are global by design and must never be filtered by tenant.
     */
    private static final Set<String> IGNORED_TABLES = Set.of("sys_tenant", "sys_role", "sys_permission",
            "sys_role_permission", "flyway_schema_history");

    private TenantConstants() {
    }

    /**
     * Indicates whether the given table should bypass tenant filtering.
     *
     * @param tableName physical table name
     * @return {@code true} when the table is global
     */
    public static boolean isIgnoredTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        return IGNORED_TABLES.contains(tableName.toLowerCase(Locale.ROOT));
    }
}
