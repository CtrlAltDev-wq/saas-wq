package com.wq.server.service;

import com.wq.server.vo.SubscriptionVO;

/**
 * Subscription queries of the current tenant.
 */
public interface SubscriptionService {

    /**
     * Returns the current subscription of the authenticated tenant.
     *
     * @return subscription
     */
    SubscriptionVO current();
}
