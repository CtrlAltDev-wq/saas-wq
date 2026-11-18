package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wq.common.api.PageResult;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.security.SecurityUtils;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.dto.UserCreateRequest;
import com.wq.server.dto.UserUpdateRequest;
import com.wq.server.entity.SysUser;
import com.wq.server.enums.UserStatus;
import com.wq.server.mapper.SysUserMapper;
import com.wq.server.service.UserService;
import com.wq.server.vo.UserVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * User management implementation. All statements are automatically restricted to the
 * tenant bound in {@link TenantContext}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserVO> records = result.getRecords().stream().map(UserVO::from).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public UserVO getById(Long id) {
        return UserVO.from(getOrThrow(id));
    }

    @Override
    @Transactional
    public UserVO create(UserCreateRequest request) {
        long existing = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (existing > 0L) {
            throw new BusinessException(ResultCode.CONFLICT, "Username already exists in this tenant");
        }
        SysUser user = new SysUser();
        user.setTenantId(TenantContext.getRequiredTenantId());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE.name());
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "Username already exists in this tenant");
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = getOrThrow(id);
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setStatus(request.status());
        sysUserMapper.updateById(user);
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (SecurityUtils.getCurrentUserId().equals(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "You cannot delete your own account");
        }
        getOrThrow(id);
        sysUserMapper.deleteById(id);
    }

    private SysUser getOrThrow(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found in the current tenant");
        }
        return user;
    }
}
