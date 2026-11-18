package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Association row granting one permission to one role.
 *
 * <p>The table is global by design, so it is excluded from tenant row filtering in
 * {@link com.wq.framework.tenant.TenantConstants}.</p>
 */
@Getter
@Setter
@TableName("sys_role_permission")
public class SysRolePermission {

    /**
     * Database generated primary key.
     */
    @TableId
    private Long id;

    /**
     * Role identifier.
     */
    private Long roleId;

    /**
     * Permission identifier.
     */
    private Long permissionId;
}
