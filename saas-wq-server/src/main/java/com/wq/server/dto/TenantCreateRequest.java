package com.wq.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Tenant creation payload.
 *
 * @param code         unique code, immutable after creation
 * @param name         display name
 * @param contactEmail administrator contact mailbox
 */
public record TenantCreateRequest(
        @NotBlank(message = "Tenant code is required")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,63}$",
                message = "Tenant code must be 3-64 characters of lowercase letters, digits or hyphens")
        String code,

        @NotBlank(message = "Tenant name is required")
        @Size(max = 128, message = "Tenant name must not exceed 128 characters")
        String name,

        @Email(message = "Contact email is invalid")
        @Size(max = 128, message = "Contact email must not exceed 128 characters")
        String contactEmail) {
}
