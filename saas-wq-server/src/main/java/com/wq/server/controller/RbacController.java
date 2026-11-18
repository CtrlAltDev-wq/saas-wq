package com.wq.server.controller;

import com.wq.common.api.Result;
import com.wq.framework.audit.Audit;
import com.wq.server.dto.RolePermissionUpdateRequest;
import com.wq.server.service.RbacService;
import com.wq.server.vo.PermissionVO;
import com.wq.server.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role and permission administration endpoints.
 */
@Tag(name = "RBAC", description = "Roles, permissions and role permission assignment")
@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    /**
     * Lists every role with its granted permission codes.
     *
     * @return roles
     */
    @Operation(summary = "List roles with their permissions")
    @PreAuthorize("hasAuthority('rbac:read')")
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.success(rbacService.roles());
    }

    /**
     * Lists the permission catalogue.
     *
     * @return permissions
     */
    @Operation(summary = "List the permission catalogue")
    @PreAuthorize("hasAuthority('rbac:read')")
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> permissions() {
        return Result.success(rbacService.permissions());
    }

    /**
     * Replaces the permission set of a role. The change is visible on the next request.
     *
     * @param code    role code
     * @param request replacement permission codes
     * @return updated role
     */
    @Operation(summary = "Replace the permission set of a role")
    @PreAuthorize("hasAuthority('rbac:write')")
    @Audit(action = "RBAC_UPDATE", resource = "RBAC")
    @PutMapping("/roles/{code}/permissions")
    public Result<RoleVO> updatePermissions(@PathVariable String code,
                                            @Valid @RequestBody RolePermissionUpdateRequest request) {
        return Result.success(rbacService.updatePermissions(code, request.permissionCodes()));
    }
}
