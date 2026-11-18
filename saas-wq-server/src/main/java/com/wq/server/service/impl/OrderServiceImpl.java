package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wq.common.api.PageResult;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.server.dto.CheckoutRequest;
import com.wq.server.entity.PaymentOrder;
import com.wq.server.mapper.PaymentOrderMapper;
import com.wq.server.payment.StripeService;
import com.wq.server.service.OrderService;
import com.wq.server.vo.CheckoutSessionVO;
import com.wq.server.vo.OrderVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Order management implementation. Queries rely on the tenant plugin for isolation.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final PaymentOrderMapper orderMapper;
    private final StripeService stripeService;

    @Override
    public PageResult<OrderVO> page(long page, long size) {
        LambdaQueryWrapper<PaymentOrder> wrapper = Wrappers.<PaymentOrder>lambdaQuery()
                .orderByDesc(PaymentOrder::getCreatedAt);
        Page<PaymentOrder> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<OrderVO> records = result.getRecords().stream().map(OrderVO::from).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public OrderVO getById(Long id) {
        PaymentOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order not found in the current tenant");
        }
        return OrderVO.from(order);
    }

    @Override
    public CheckoutSessionVO checkout(CheckoutRequest request) {
        return stripeService.createCheckoutSession(request.customerId(), request.planCode());
    }
}
