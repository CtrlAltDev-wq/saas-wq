package com.wq.server.controller;

import com.wq.common.api.PageResult;
import com.wq.common.api.Result;
import com.wq.server.dto.CheckoutRequest;
import com.wq.server.service.OrderService;
import com.wq.server.vo.CheckoutSessionVO;
import com.wq.server.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order and subscription checkout endpoints scoped to the authenticated tenant.
 */
@Tag(name = "Orders", description = "Tenant scoped orders and Stripe checkout")
@Validated
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MEMBER')")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Lists orders of the current tenant.
     *
     * @param page one based page index
     * @param size page size
     * @return paged orders
     */
    @Operation(summary = "List orders of the current tenant")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") long size) {
        return Result.success(orderService.page(page, size));
    }

    /**
     * Loads an order of the current tenant.
     *
     * @param id order identifier
     * @return order
     */
    @Operation(summary = "Get an order by identifier")
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /**
     * Creates a Stripe Checkout subscription session for the tenant billing customer.
     *
     * @param request checkout payload
     * @return hosted checkout session
     */
    @Operation(summary = "Create a Stripe Checkout subscription session")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/checkout")
    public Result<CheckoutSessionVO> checkout(@Valid @RequestBody CheckoutRequest request) {
        return Result.success(orderService.checkout(request));
    }
}
