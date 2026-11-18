package com.wq.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Tenant update payload. The code is intentionally not updatable.
 *
 * @param name         display name
 * @param contactEmail administrator contact mailbox
 * @param status       optional ACTIVE or DISABLED
 */
public record TenantUpdateRequest(
        @NotBlank(message = "Tenant name is required")
        @Size(max = 128, message = "Tenant name must not exceed 128 characters")
        String name,

        @Email(message = "Contact email is invalid")
        @Size(max = 128, message = "Contact email must not exceed 128 characters")
        String contactEmail,

        @Pattern(regexp = "ACTIVE|DISABLED", message = "Status must be ACTIVE or DISABLED")
        String status) {
}
