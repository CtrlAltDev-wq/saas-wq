package com.wq.server.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wq.server.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User account mapper.
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Counts users of a tenant, bypassing the current tenant filter because the caller
     * (platform administrator) is inspecting a tenant other than its own.
     *
     * @param tenantId target tenant
     * @return number of active user rows
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND deleted = 0")
    long countByTenantId(@Param("tenantId") Long tenantId);
}
