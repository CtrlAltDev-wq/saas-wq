package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant scoped user account.
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * Owning tenant, injected by the tenant interceptor on writes and filters on reads.
     */
    private Long tenantId;

    /**
     * Login name, unique within one tenant.
     */
    private String username;

    /**
     * BCrypt password hash, never serialised.
     */
    @JsonIgnore
    private String password;

    /**
     * Display name.
     */
    private String nickname;

    /**
     * Contact mailbox.
     */
    private String email;

    /**
     * {@link com.wq.server.enums.UserRole} value.
     */
    private String role;

    /**
     * {@link com.wq.server.enums.UserStatus} value.
     */
    private String status;
}
