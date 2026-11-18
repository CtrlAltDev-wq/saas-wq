package com.wq.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload. A tenant code is mandatory because the same username may exist in
 * several tenants.
 *
 * @param tenantCode unique tenant code
 * @param username   login name inside the tenant
 * @param password   raw password
 */
@Schema(description = "Login request scoped to a tenant")
public record LoginRequest(
        @Schema(description = "Unique tenant code", example = "acme")
        @NotBlank(message = "Tenant code is required")
        String tenantCode,

        @Schema(description = "Login name inside the tenant", example = "acme.admin")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Raw password", example = "Admin@123")
        @NotBlank(message = "Password is required")
        String password) {
}
