package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.security.JwtPayload;
import com.wq.framework.security.JwtTokenProvider;
import com.wq.framework.security.LoginUser;
import com.wq.framework.security.SecurityUtils;
import com.wq.framework.security.TokenBlacklistService;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.dto.LoginRequest;
import com.wq.server.entity.SysUser;
import com.wq.server.entity.Tenant;
import com.wq.server.enums.TenantStatus;
import com.wq.server.enums.UserStatus;
import com.wq.server.mapper.SysUserMapper;
import com.wq.server.mapper.TenantMapper;
import com.wq.server.service.AuthService;
import com.wq.server.vo.LoginVO;
import com.wq.server.vo.UserVO;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Authentication implementation.
 *
 * <p>Login resolves the tenant first, binds it to the current thread, and only then
 * queries the user. That way the multi-tenant plugin enforces isolation even during
 * authentication, instead of relying on an ad hoc {@code tenant_id} predicate.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TenantMapper tenantMapper;

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public LoginVO login(LoginRequest request) {
        Tenant tenant = tenantMapper.selectOne(Wrappers.<Tenant>lambdaQuery()
                .eq(Tenant::getCode, request.tenantCode()));
        if (tenant == null) {
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        if (!TenantStatus.ACTIVE.name().equals(tenant.getStatus())) {
            throw new BusinessException(ResultCode.TENANT_DISABLED);
        }
        TenantContext.set(tenant.getId());
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        LoginUser principal = new LoginUser(user.getId(), user.getTenantId(), user.getUsername(), user.getRole());
        String accessToken = jwtTokenProvider.generateToken(principal);
        return new LoginVO(accessToken, "Bearer", jwtTokenProvider.getExpiration().toSeconds(), UserVO.from(user));
    }

    @Override
    public void logout(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return;
        }
        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtPayload payload = jwtTokenProvider.parseToken(accessToken);
            Duration remainingLifetime = Duration.between(Instant.now(), payload.expiresAt());
            tokenBlacklistService.blacklist(payload.tokenId(), remainingLifetime);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Skipping logout for an invalid access token");
        }
    }

    @Override
    public UserVO currentUser() {
        SysUser user = sysUserMapper.selectById(SecurityUtils.getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Authenticated user no longer exists");
        }
        return UserVO.from(user);
    }
}
