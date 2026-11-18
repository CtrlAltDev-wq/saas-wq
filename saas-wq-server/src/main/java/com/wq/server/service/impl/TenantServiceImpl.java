package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wq.common.api.PageResult;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.tenant.TenantConstants;
import com.wq.server.dto.TenantCreateRequest;
import com.wq.server.dto.TenantUpdateRequest;
import com.wq.server.entity.Tenant;
import com.wq.server.enums.TenantStatus;
import com.wq.server.mapper.SysUserMapper;
import com.wq.server.mapper.TenantMapper;
import com.wq.server.service.TenantService;
import com.wq.server.vo.TenantVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Tenant management implementation. Every method is reachable only by platform
 * administrators, as enforced by {@code @PreAuthorize} on the controller.
 */
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<TenantVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Tenant::getName, keyword).or().like(Tenant::getCode, keyword));
        }
        wrapper.orderByAsc(Tenant::getId);
        Page<Tenant> result = tenantMapper.selectPage(new Page<>(page, size), wrapper);
        List<TenantVO> records = result.getRecords().stream().map(TenantVO::from).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public TenantVO getById(Long id) {
        return TenantVO.from(getOrThrow(id));
    }

    @Override
    @Transactional
    public TenantVO create(TenantCreateRequest request) {
        if (tenantMapper.countByCodeIncludingDeleted(request.code()) > 0L) {
            throw new BusinessException(ResultCode.CONFLICT, "Tenant code already exists: " + request.code());
        }
        Tenant tenant = new Tenant();
        tenant.setCode(request.code());
        tenant.setName(request.name());
        tenant.setContactEmail(request.contactEmail());
        tenant.setStatus(TenantStatus.ACTIVE.name());
        try {
            tenantMapper.insert(tenant);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "Tenant code already exists: " + request.code());
        }
        return TenantVO.from(tenant);
    }

    @Override
    @Transactional
    public TenantVO update(Long id, TenantUpdateRequest request) {
        Tenant tenant = getOrThrow(id);
        tenant.setName(request.name());
        tenant.setContactEmail(request.contactEmail());
        if (StringUtils.hasText(request.status())) {
            tenant.setStatus(request.status());
        }
        tenantMapper.updateById(tenant);
        return TenantVO.from(tenant);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null || id == TenantConstants.PLATFORM_TENANT_ID) {
            throw new BusinessException(ResultCode.PLATFORM_TENANT_PROTECTED);
        }
        getOrThrow(id);
        long userCount = sysUserMapper.countByTenantId(id);
        if (userCount > 0L) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "Tenant still owns " + userCount + " user(s) and cannot be deleted");
        }
        tenantMapper.deleteById(id);
    }

    private Tenant getOrThrow(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }
}
