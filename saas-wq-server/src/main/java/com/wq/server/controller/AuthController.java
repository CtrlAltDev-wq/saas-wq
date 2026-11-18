package com.wq.server.controller;

import com.wq.common.api.Result;
import com.wq.framework.audit.Audit;
import com.wq.server.dto.LoginRequest;
import com.wq.server.service.AuthService;
import com.wq.server.vo.LoginVO;
import com.wq.server.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 */
@Tag(name = "Authentication", description = "Tenant scoped login, logout and profile")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user against a tenant and issues a JWT.
     *
     * @param request login payload
     * @return access token and profile
     */
    @Operation(summary = "Login with tenant code, username and password")
    @Audit(action = "AUTH_LOGIN", resource = "AUTH")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * Revokes the presented access token.
     *
     * @param authorizationHeader raw Authorization header
     * @return empty success envelope
     */
    @Operation(summary = "Logout and revoke the current access token")
    @Audit(action = "AUTH_LOGOUT", resource = "AUTH")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return Result.success();
    }

    /**
     * Returns the authenticated profile.
     *
     * @return profile of the current user
     */
    @Operation(summary = "Get the authenticated user profile")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser());
    }
}
