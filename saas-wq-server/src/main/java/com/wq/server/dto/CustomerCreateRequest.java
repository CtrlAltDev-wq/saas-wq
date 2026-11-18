package com.wq.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Customer creation payload.
 *
 * @param name    contact name
 * @param company company name
 * @param email   contact mailbox
 * @param phone   contact phone number
 * @param status  ACTIVE or INACTIVE
 * @param remark  free-form notes
 */
public record CustomerCreateRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 128, message = "Customer name must not exceed 128 characters")
        String name,

        @Size(max = 128, message = "Company must not exceed 128 characters")
        String company,

        @Email(message = "Email is invalid")
        @Size(max = 128, message = "Email must not exceed 128 characters")
        String email,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
        String status,

        @Size(max = 512, message = "Remark must not exceed 512 characters")
        String remark) {
}
