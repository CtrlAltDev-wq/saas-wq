package com.wq.server.service;

import com.wq.common.api.PageResult;
import com.wq.server.dto.TenantCreateRequest;
import com.wq.server.dto.TenantUpdateRequest;
import com.wq.server.vo.TenantVO;

/**
 * Tenant management use cases, reserved for platform administrators.
 */
public interface TenantService {

    /**
     * Lists tenants with pagination and an optional keyword filter.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on code or name
     * @return paged tenants
     */
    PageResult<TenantVO> page(long page, long size, String keyword);

    /**
     * Loads a single tenant.
     *
     * @param id tenant identifier
     * @return tenant profile
     */
    TenantVO getById(Long id);

    /**
     * Registers a new tenant.
     *
     * @param request creation payload
     * @return created tenant
     */
    TenantVO create(TenantCreateRequest request);

    /**
     * Updates display information of a tenant.
     *
     * @param id      tenant identifier
     * @param request update payload
     * @return updated tenant
     */
    TenantVO update(Long id, TenantUpdateRequest request);

    /**
     * Logically deletes a tenant that has no users.
     *
     * @param id tenant identifier
     */
    void delete(Long id);
}
