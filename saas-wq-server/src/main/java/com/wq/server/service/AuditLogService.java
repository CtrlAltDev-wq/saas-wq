package com.wq.server.service;

import com.wq.common.api.PageResult;
import com.wq.server.entity.SysAuditLog;
import com.wq.server.vo.AuditLogVO;

/**
 * Audit trail access.
 */
public interface AuditLogService {

    /**
     * Writes one audit entry. Failures are swallowed by the caller so that auditing can
     * never break a business operation.
     *
     * @param entry audit entry
     */
    void record(SysAuditLog entry);

    /**
     * Pages the audit trail of the current tenant.
     *
     * @param page     one based page index
     * @param size     page size
     * @param action   optional exact action match
     * @param username optional partial username match
     * @param success  optional outcome filter
     * @return paged audit entries ordered from newest to oldest
     */
    PageResult<AuditLogVO> page(long page, long size, String action, String username, Boolean success);
}
