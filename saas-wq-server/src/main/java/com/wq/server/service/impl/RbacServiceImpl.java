package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.server.entity.SysPermission;
import com.wq.server.entity.SysRole;
import com.wq.server.entity.SysRolePermission;
import com.wq.server.mapper.SysPermissionMapper;
import com.wq.server.mapper.SysRoleMapper;
import com.wq.server.mapper.SysRolePermissionMapper;
import com.wq.server.service.PermissionService;
import com.wq.server.service.RbacService;
import com.wq.server.vo.PermissionVO;
import com.wq.server.vo.RoleVO;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role and permission administration backed by the global RBAC tables.
 *
 * <p>Every mutation evicts the Redis cache of the affected role, so the next authenticated
 * request observes the new permission set without waiting for the cache to expire.</p>
 */
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysRoleMapper roleMapper;

    private final SysPermissionMapper permissionMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final PermissionService permissionService;

    @Override
    public List<RoleVO> roles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId))
                .stream()
                .map(role -> RoleVO.from(role, rolePermissionMapper.selectPermissionCodesByRole(role.getCode())))
                .toList();
    }

    @Override
    public List<PermissionVO> permissions() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getResource)
                        .orderByAsc(SysPermission::getAction))
                .stream()
                .map(PermissionVO::from)
                .toList();
    }

    @Override
    @Transactional
    public RoleVO updatePermissions(String roleCode, List<String> permissionCodes) {
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, roleCode));
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Role not found: " + roleCode);
        }
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "Permissions of SUPER_ADMIN are immutable to prevent locking the platform");
        }
        List<SysPermission> catalogue = permissionMapper.selectList(null);
        Map<String, SysPermission> byCode = catalogue.stream()
                .collect(Collectors.toMap(SysPermission::getCode, Function.identity()));
        List<String> requested = permissionCodes.stream().distinct().toList();
        List<String> unknown = requested.stream().filter(code -> !byCode.containsKey(code)).toList();
        if (!unknown.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unknown permission codes: " + String.join(", ", unknown));
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, role.getId()));
        for (String code : requested) {
            SysRolePermission mapping = new SysRolePermission();
            mapping.setRoleId(role.getId());
            mapping.setPermissionId(byCode.get(code).getId());
            rolePermissionMapper.insert(mapping);
        }
        permissionService.evict(role.getCode());
        return RoleVO.from(role, requested.stream().sorted().toList());
    }
}
