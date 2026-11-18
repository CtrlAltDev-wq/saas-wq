package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Role definition shared by every tenant.
 *
 * <p>The code is the stable identifier referenced by {@code sys_user.role} and embedded
 * into access tokens, so it must never change once assigned.</p>
 */
@Getter
@Setter
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /**
     * Stable role code, for example TENANT_ADMIN.
     */
    private String code;

    /**
     * Display name.
     */
    private String name;

    /**
     * Human readable purpose.
     */
    private String description;

    /**
     * Built-in roles cannot be renamed or removed.
     */
    private Boolean builtIn;
}
