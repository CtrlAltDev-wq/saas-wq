package com.wq.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wq.server.entity.SysRolePermission;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Role permission mapping persistence.
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * Returns the permission codes granted to a role.
     *
     * @param roleCode role code
     * @return permission codes ordered by code
     */
    @Select("""
            SELECT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_role r ON r.id = rp.role_id
            WHERE r.code = #{roleCode} AND r.deleted = 0 AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByRole(@Param("roleCode") String roleCode);
}
