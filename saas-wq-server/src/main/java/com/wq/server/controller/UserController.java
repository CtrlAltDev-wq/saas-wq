package com.wq.server.controller;

import com.wq.common.api.PageResult;
import com.wq.common.api.Result;
import com.wq.server.dto.UserCreateRequest;
import com.wq.server.dto.UserUpdateRequest;
import com.wq.server.service.UserService;
import com.wq.server.vo.UserVO;
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
 * User management endpoints scoped to the authenticated tenant.
 */
@Tag(name = "Users", description = "Tenant scoped user management")
@Validated
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lists users of the current tenant.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on username or nickname
     * @return paged users
     */
    @Operation(summary = "List users of the current tenant")
    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.page(page, size, keyword));
    }

    /**
     * Loads a user of the current tenant.
     *
     * @param id user identifier
     * @return user profile
     */
    @Operation(summary = "Get a user by identifier")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * Creates a user inside the current tenant.
     *
     * @param request creation payload
     * @return created user
     */
    @Operation(summary = "Create a user in the current tenant")
    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    /**
     * Updates a user of the current tenant.
     *
     * @param id      user identifier
     * @param request update payload
     * @return updated user
     */
    @Operation(summary = "Update a user in the current tenant")
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    /**
     * Logically deletes a user of the current tenant.
     *
     * @param id user identifier
     * @return empty success envelope
     */
    @Operation(summary = "Delete a user from the current tenant")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}
