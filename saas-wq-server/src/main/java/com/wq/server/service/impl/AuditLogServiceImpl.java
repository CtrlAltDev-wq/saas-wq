package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wq.common.api.PageResult;
import com.wq.server.entity.SysAuditLog;
import com.wq.server.mapper.SysAuditLogMapper;
import com.wq.server.service.AuditLogService;
import com.wq.server.vo.AuditLogVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Audit trail persistence and querying. All statements are restricted to the tenant bound
 * in the request context by the MyBatis-Plus tenant interceptor.
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    @Override
    public void record(SysAuditLog entry) {
        auditLogMapper.insert(entry);
    }

    @Override
    public PageResult<AuditLogVO> page(long page, long size, String action, String username, Boolean success) {
        LambdaQueryWrapper<SysAuditLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(action)) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(SysAuditLog::getUsername, username);
        }
        if (success != null) {
            wrapper.eq(SysAuditLog::getSuccess, success);
        }
        wrapper.orderByDesc(SysAuditLog::getId);
        Page<SysAuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<AuditLogVO> records = result.getRecords().stream().map(AuditLogVO::from).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }
}
