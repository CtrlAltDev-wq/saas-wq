package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.server.entity.TenantSubscription;
import com.wq.server.mapper.TenantSubscriptionMapper;
import com.wq.server.service.SubscriptionService;
import com.wq.server.vo.SubscriptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Subscription query implementation.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TenantSubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionVO current() {
        TenantSubscription subscription = subscriptionMapper.selectOne(
                Wrappers.<TenantSubscription>lambdaQuery());
        if (subscription == null) {
            throw new BusinessException(ResultCode.SUBSCRIPTION_NOT_FOUND);
        }
        return SubscriptionVO.from(subscription);
    }
}
