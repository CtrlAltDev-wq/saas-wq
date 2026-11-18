package com.wq.server.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wq.server.entity.TenantSubscription;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Subscription persistence. The global lookup supports webhook processing before a
 * tenant context is established.
 */
public interface TenantSubscriptionMapper extends BaseMapper<TenantSubscription> {

    /**
     * Finds a subscription by its Stripe identifier across all tenants.
     *
     * @param subscriptionId Stripe subscription identifier
     * @return matching subscription or {@code null}
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM biz_subscription WHERE stripe_subscription_id = #{subscriptionId} AND deleted = 0 LIMIT 1")
    TenantSubscription selectBySubscriptionIdGlobally(@Param("subscriptionId") String subscriptionId);
}
