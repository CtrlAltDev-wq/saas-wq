package com.wq.server.controller;

import com.wq.common.api.Result;
import com.wq.server.service.SubscriptionService;
import com.wq.server.vo.SubscriptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscription endpoints scoped to the authenticated tenant.
 */
@Tag(name = "Subscriptions", description = "Tenant subscription entitlement")
@RestController
@RequestMapping("/api/subscriptions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MEMBER')")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Returns the current subscription and plan entitlement of the tenant.
     *
     * @return current subscription
     */
    @Operation(summary = "Get the current tenant subscription")
    @GetMapping("/current")
    public Result<SubscriptionVO> current() {
        return Result.success(subscriptionService.current());
    }
}
