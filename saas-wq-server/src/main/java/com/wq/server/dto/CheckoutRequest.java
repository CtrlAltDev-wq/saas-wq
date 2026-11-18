package com.wq.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to start a subscription checkout.
 *
 * @param customerId billing customer identifier
 * @param planCode   plan code configured under wq.stripe.plans
 */
@Schema(description = "Subscription checkout request")
public record CheckoutRequest(
        @Schema(description = "Billing customer identifier", example = "1")
        @NotNull(message = "Customer id is required")
        Long customerId,

        @Schema(description = "Plan code", example = "PROFESSIONAL")
        @NotBlank(message = "Plan code is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,32}$", message = "Plan code contains unsupported characters")
        String planCode) {
}
