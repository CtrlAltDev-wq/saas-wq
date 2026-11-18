package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant registry entry. This table is global and therefore excluded from row filtering.
 */
@Getter
@Setter
@TableName("sys_tenant")
public class Tenant extends BaseEntity {

    /**
     * Unique code used together with the username during login.
     */
    private String code;

    /**
     * Display name.
     */
    private String name;

    /**
     * Contact mailbox of the tenant administrator.
     */
    private String contactEmail;

    /**
     * {@link com.wq.server.enums.TenantStatus} value.
     */
    private String status;
}
