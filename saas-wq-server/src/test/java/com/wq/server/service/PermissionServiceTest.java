package com.wq.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wq.server.mapper.SysRolePermissionMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Permission resolution and cache behaviour.
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    private static final String MEMBER_KEY = "saas-wq:rbac:role:MEMBER";

    @Mock
    private SysRolePermissionMapper rolePermissionMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void loadsFromDatabaseAndCachesOnCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(MEMBER_KEY)).thenReturn(null);
        when(rolePermissionMapper.selectPermissionCodesByRole("MEMBER"))
                .thenReturn(List.of("customer:read", "order:read"));

        List<String> permissions = List.copyOf(service().permissionsFor("MEMBER"));

        assertEquals(List.of("customer:read", "order:read"), permissions);
        verify(valueOperations).set(MEMBER_KEY, "customer:read,order:read", Duration.ofMinutes(30));
    }

    @Test
    void returnsCachedPermissionsWithoutHittingTheDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(MEMBER_KEY)).thenReturn("customer:read,order:read");

        List<String> permissions = List.copyOf(service().permissionsFor("MEMBER"));

        assertEquals(List.of("customer:read", "order:read"), permissions);
        verifyNoInteractions(rolePermissionMapper);
    }

    @Test
    void returnsEmptyListForUnknownRole() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("saas-wq:rbac:role:GHOST")).thenReturn(null);
        when(rolePermissionMapper.selectPermissionCodesByRole("GHOST")).thenReturn(List.of());

        assertEquals(List.of(), List.copyOf(service().permissionsFor("GHOST")));
    }

    @Test
    void fallsBackToTheDatabaseWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("redis down"));
        when(rolePermissionMapper.selectPermissionCodesByRole("MEMBER")).thenReturn(List.of("customer:read"));

        assertEquals(List.of("customer:read"), List.copyOf(service().permissionsFor("MEMBER")));
    }

    @Test
    void evictsTheCachedPermissionsOfARole() {
        service().evict("MEMBER");

        verify(redisTemplate).delete(MEMBER_KEY);
    }

    private PermissionService service() {
        return new PermissionService(rolePermissionMapper, redisTemplate);
    }
}
