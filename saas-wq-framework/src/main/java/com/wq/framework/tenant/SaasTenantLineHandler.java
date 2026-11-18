package com.wq.framework.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * MyBatis-Plus tenant handler implementing row level isolation.
 *
 * <p>Every statement targeting a tenant scoped table gets an extra
 * {@code tenant_id = ?} predicate derived from {@link TenantContext}. Write statements
 * automatically receive the tenant column as well, so callers cannot forget it.</p>
 */
public class SaasTenantLineHandler implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.getRequiredTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return TenantConstants.TENANT_ID_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantConstants.isIgnoredTable(tableName);
    }
}
