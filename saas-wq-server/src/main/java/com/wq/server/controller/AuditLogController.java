package com.wq.server.controller;

import com.wq.common.api.PageResult;
import com.wq.common.api.Result;
import com.wq.server.service.AuditLogService;
import com.wq.server.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit trail query endpoints, scoped to the tenant of the caller.
 */
@Tag(name = "Audit", description = "Tenant scoped audit trail")
@Validated
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAuthority('audit:read')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Pages the audit trail of the current tenant.
     *
     * @param page     one based page index
     * @param size     page size
     * @param action   optional exact action match
     * @param username optional partial username match
     * @param success  optional outcome filter
     * @return paged audit entries
     */
    @Operation(summary = "List the audit trail of the current tenant")
    @GetMapping
    public Result<PageResult<AuditLogVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") long size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean success) {
        return Result.success(auditLogService.page(page, size, action, username, success));
    }
}
