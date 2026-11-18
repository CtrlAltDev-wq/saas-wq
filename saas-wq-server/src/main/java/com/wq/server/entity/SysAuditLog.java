package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * One entry of the tenant audit trail.
 *
 * <p>The row is tenant scoped, so a tenant administrator can only browse the operations
 * performed inside their own tenant. Creation time is written explicitly by the aspect
 * instead of relying on the MyBatis audit handler.</p>
 */
@Getter
@Setter
@TableName("sys_audit_log")
public class SysAuditLog {

    /**
     * Database generated primary key.
     */
    @TableId
    private Long id;

    /**
     * Owning tenant.
     */
    private Long tenantId;

    /**
     * Actor identifier, absent when authentication failed before the user was resolved.
     */
    private Long userId;

    /**
     * Actor login name.
     */
    private String username;

    /**
     * Business action, for example CUSTOMER_CREATE.
     */
    private String action;

    /**
     * Affected resource, for example CUSTOMER.
     */
    private String resource;

    /**
     * HTTP method of the audited request.
     */
    private String httpMethod;

    /**
     * Request URI of the audited request.
     */
    private String uri;

    /**
     * Sanitised and truncated request arguments.
     */
    private String requestArgs;

    /**
     * Whether the operation completed successfully.
     */
    private Boolean success;

    /**
     * Error message of a failed operation.
     */
    private String errorMessage;

    /**
     * Execution time in milliseconds.
     */
    private Long durationMs;

    /**
     * Client address.
     */
    private String ipAddress;

    /**
     * Creation time.
     */
    private LocalDateTime createdAt;
}
