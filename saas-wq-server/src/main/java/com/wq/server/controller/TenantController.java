package com.wq.server.controller;

import com.wq.common.api.PageResult;
import com.wq.common.api.Result;
import com.wq.framework.audit.Audit;
import com.wq.server.dto.TenantCreateRequest;
import com.wq.server.dto.TenantUpdateRequest;
import com.wq.server.service.TenantService;
import com.wq.server.vo.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant registry endpoints, reserved for platform administrators.
 */
@Tag(name = "Tenants", description = "Platform level tenant registry")
@Validated
@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasAuthority('tenant:read')")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * Lists tenants with pagination.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on code or name
     * @return paged tenants
     */
    @Operation(summary = "List tenants with pagination")
    @GetMapping
    public Result<PageResult<TenantVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(tenantService.page(page, size, keyword));
    }

    /**
     * Loads a single tenant.
     *
     * @param id tenant identifier
     * @return tenant profile
     */
    @Operation(summary = "Get a tenant by identifier")
    @GetMapping("/{id}")
    public Result<TenantVO> getById(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }

    /**
     * Registers a new tenant.
     *
     * @param request creation payload
     * @return created tenant
     */
    @Operation(summary = "Create a tenant")
    @PreAuthorize("hasAuthority('tenant:write')")
    @Audit(action = "TENANT_CREATE", resource = "TENANT")
    @PostMapping
    public Result<TenantVO> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.success(tenantService.create(request));
    }

    /**
     * Updates tenant display information.
     *
     * @param id      tenant identifier
     * @param request update payload
     * @return updated tenant
     */
    @Operation(summary = "Update a tenant")
    @PreAuthorize("hasAuthority('tenant:write')")
    @Audit(action = "TENANT_UPDATE", resource = "TENANT")
    @PutMapping("/{id}")
    public Result<TenantVO> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateRequest request) {
        return Result.success(tenantService.update(id, request));
    }

    /**
     * Logically deletes a tenant that has no users.
     *
     * @param id tenant identifier
     * @return empty success envelope
     */
    @Operation(summary = "Delete a tenant")
    @PreAuthorize("hasAuthority('tenant:write')")
    @Audit(action = "TENANT_DELETE", resource = "TENANT")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return Result.success();
    }
}
