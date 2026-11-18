package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Permission definition. Every code is used verbatim as a Spring Security authority.
 */
@Getter
@Setter
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /**
     * Permission code following the {@code resource:action} convention.
     */
    private String code;

    /**
     * Display name.
     */
    private String name;

    /**
     * Protected resource, for example customer.
     */
    private String resource;

    /**
     * Allowed action, for example read or write.
     */
    private String action;

    /**
     * Human readable purpose.
     */
    private String description;
}
