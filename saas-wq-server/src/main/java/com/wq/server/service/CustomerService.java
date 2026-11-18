package com.wq.server.service;

import com.wq.common.api.PageResult;
import com.wq.server.dto.CustomerCreateRequest;
import com.wq.server.dto.CustomerUpdateRequest;
import com.wq.server.vo.CustomerVO;

/**
 * Customer management use cases scoped to the authenticated tenant.
 */
public interface CustomerService {

    /**
     * Lists customers of the current tenant.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on name or company
     * @return paged customers
     */
    PageResult<CustomerVO> page(long page, long size, String keyword);

    /**
     * Loads a customer of the current tenant.
     *
     * @param id customer identifier
     * @return customer profile
     */
    CustomerVO getById(Long id);

    /**
     * Creates a customer inside the current tenant.
     *
     * @param request creation payload
     * @return created customer
     */
    CustomerVO create(CustomerCreateRequest request);

    /**
     * Updates a customer of the current tenant.
     *
     * @param id      customer identifier
     * @param request update payload
     * @return updated customer
     */
    CustomerVO update(Long id, CustomerUpdateRequest request);

    /**
     * Logically deletes a customer of the current tenant.
     *
     * @param id customer identifier
     */
    void delete(Long id);
}
