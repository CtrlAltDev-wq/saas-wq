package com.wq.server.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wq.server.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Order persistence. The global lookups are used by webhook processing, where no
 * tenant context exists yet and the owning tenant is unknown until the row is loaded.
 */
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * Finds an order by its Stripe Checkout session identifier across all tenants.
     *
     * @param sessionId Stripe Checkout session identifier
     * @return matching order or {@code null}
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM biz_order WHERE stripe_checkout_session_id = #{sessionId} AND deleted = 0 LIMIT 1")
    PaymentOrder selectByCheckoutSessionIdGlobally(@Param("sessionId") String sessionId);

    /**
     * Finds an order by its Stripe subscription identifier across all tenants.
     *
     * @param subscriptionId Stripe subscription identifier
     * @return matching order or {@code null}
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM biz_order WHERE stripe_subscription_id = #{subscriptionId} AND deleted = 0 LIMIT 1")
    PaymentOrder selectBySubscriptionIdGlobally(@Param("subscriptionId") String subscriptionId);

    /**
     * Finds an order by its primary key across all tenants.
     *
     * @param id order identifier
     * @return matching order or {@code null}
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM biz_order WHERE id = #{id} AND deleted = 0 LIMIT 1")
    PaymentOrder selectByIdGlobally(@Param("id") Long id);
}
