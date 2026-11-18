package com.wq.server.service;

import com.wq.framework.security.PermissionLookup;
import com.wq.server.mapper.SysRolePermissionMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves the permission codes of a role and caches the result in Redis.
 *
 * <p>Permissions are resolved per request instead of being embedded into the access token,
 * so a role change takes effect on the next request without forcing users to log in again.
 * The cache is evicted explicitly whenever the role permission set is edited.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService implements PermissionLookup {

    /**
     * Redis namespace of the role permission cache.
     */
    static final String CACHE_PREFIX = "saas-wq:rbac:role:";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final SysRolePermissionMapper rolePermissionMapper;

    private final StringRedisTemplate redisTemplate;

    @Override
    public Collection<String> permissionsFor(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        String cacheKey = CACHE_PREFIX + roleCode;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return split(cached);
            }
        } catch (DataAccessException exception) {
            log.warn("Role permission cache unavailable for {}, falling back to the database: {}",
                    roleCode, exception.getMessage());
        }
        List<String> permissions = rolePermissionMapper.selectPermissionCodesByRole(roleCode);
        if (permissions == null) {
            permissions = List.of();
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, String.join(",", permissions), CACHE_TTL);
        } catch (DataAccessException exception) {
            log.warn("Unable to cache permissions of role {}: {}", roleCode, exception.getMessage());
        }
        return permissions;
    }

    /**
     * Drops the cached permissions of a role so the next request reloads them.
     *
     * @param roleCode role code
     */
    public void evict(String roleCode) {
        try {
            redisTemplate.delete(CACHE_PREFIX + roleCode);
        } catch (DataAccessException exception) {
            log.warn("Unable to evict permissions of role {}: {}", roleCode, exception.getMessage());
        }
    }

    private List<String> split(String cached) {
        if (!StringUtils.hasText(cached)) {
            return List.of();
        }
        return Arrays.stream(cached.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }
}
