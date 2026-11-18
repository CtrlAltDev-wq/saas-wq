package com.wq.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Replacement permission set of a role.
 *
 * @param permissionCodes permission codes that must be granted to the role
 */
@Schema(description = "Replacement permission set of a role")
public record RolePermissionUpdateRequest(
        @Schema(description = "Permission codes to grant", example = "[\"customer:read\", \"customer:write\"]")
        @NotEmpty(message = "At least one permission code is required")
        List<String> permissionCodes) {
}
