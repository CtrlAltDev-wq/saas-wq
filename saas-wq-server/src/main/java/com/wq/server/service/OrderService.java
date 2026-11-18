package com.wq.server.service;

import com.wq.common.api.PageResult;
import com.wq.server.dto.CheckoutRequest;
import com.wq.server.vo.CheckoutSessionVO;
import com.wq.server.vo.OrderVO;

/**
 * Order management of the current tenant.
 */
public interface OrderService {

    /**
     * Lists orders of the current tenant, newest first.
     *
     * @param page one based page index
     * @param size page size
     * @return paged orders
     */
    PageResult<OrderVO> page(long page, long size);

    /**
     * Loads an order of the current tenant.
     *
     * @param id order identifier
     * @return order
     */
    OrderVO getById(Long id);

    /**
     * Starts a subscription checkout.
     *
     * @param request checkout payload
     * @return Stripe Checkout session
     */
    CheckoutSessionVO checkout(CheckoutRequest request);
}
