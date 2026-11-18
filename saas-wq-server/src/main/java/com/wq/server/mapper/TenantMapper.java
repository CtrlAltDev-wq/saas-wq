package com.wq.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wq.server.entity.Tenant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Tenant registry mapper. The table is global, so tenant filtering does not apply.
 */
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * Counts tenants owning a code, including logically deleted rows, so that a code is
     * never reused after deletion.
     *
     * @param code tenant code
     * @return number of rows owning the code
     */
    @Select("SELECT COUNT(*) FROM sys_tenant WHERE code = #{code}")
    long countByCodeIncludingDeleted(@Param("code") String code);
}
