package com.wq.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User creation payload. The tenant is derived from the authenticated principal.
 *
 * @param username login name
 * @param password raw password, hashed before persistence
 * @param nickname display name
 * @param email    contact mailbox
 * @param role     TENANT_ADMIN or MEMBER
 */
public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,64}$",
                message = "Username must be 3-64 characters of letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
        String password,

        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Email(message = "Email is invalid")
        @Size(max = 128, message = "Email must not exceed 128 characters")
        String email,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "TENANT_ADMIN|MEMBER", message = "Role must be TENANT_ADMIN or MEMBER")
        String role) {
}
