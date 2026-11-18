package com.wq.server.controller;

import com.wq.common.api.PageResult;
import com.wq.common.api.Result;
import com.wq.server.dto.CustomerCreateRequest;
import com.wq.server.dto.CustomerUpdateRequest;
import com.wq.server.service.CustomerService;
import com.wq.server.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer endpoints. Members may read, administrators may write.
 */
@Tag(name = "Customers", description = "Tenant scoped customer management")
@Validated
@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MEMBER')")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Lists customers of the current tenant.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on name or company
     * @return paged customers
     */
    @Operation(summary = "List customers of the current tenant")
    @GetMapping
    public Result<PageResult<CustomerVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(customerService.page(page, size, keyword));
    }

    /**
     * Loads a customer of the current tenant.
     *
     * @param id customer identifier
     * @return customer profile
     */
    @Operation(summary = "Get a customer by identifier")
    @GetMapping("/{id}")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    /**
     * Creates a customer inside the current tenant.
     *
     * @param request creation payload
     * @return created customer
     */
    @Operation(summary = "Create a customer in the current tenant")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @PostMapping
    public Result<CustomerVO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return Result.success(customerService.create(request));
    }

    /**
     * Updates a customer of the current tenant.
     *
     * @param id      customer identifier
     * @param request update payload
     * @return updated customer
     */
    @Operation(summary = "Update a customer in the current tenant")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/{id}")
    public Result<CustomerVO> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return Result.success(customerService.update(id, request));
    }

    /**
     * Logically deletes a customer of the current tenant.
     *
     * @param id customer identifier
     * @return empty success envelope
     */
    @Operation(summary = "Delete a customer from the current tenant")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
