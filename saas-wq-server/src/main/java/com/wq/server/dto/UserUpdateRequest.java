package com.wq.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User update payload. Credentials are managed separately from the profile.
 *
 * @param nickname display name
 * @param email    contact mailbox
 * @param role     TENANT_ADMIN or MEMBER
 * @param status   ACTIVE or DISABLED
 */
public record UserUpdateRequest(
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Email(message = "Email is invalid")
        @Size(max = 128, message = "Email must not exceed 128 characters")
        String email,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "TENANT_ADMIN|MEMBER", message = "Role must be TENANT_ADMIN or MEMBER")
        String role,

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|DISABLED", message = "Status must be ACTIVE or DISABLED")
        String status) {
}
