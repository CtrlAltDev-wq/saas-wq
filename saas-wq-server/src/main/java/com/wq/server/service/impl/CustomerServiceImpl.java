package com.wq.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wq.common.api.PageResult;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.framework.tenant.TenantContext;
import com.wq.server.dto.CustomerCreateRequest;
import com.wq.server.dto.CustomerUpdateRequest;
import com.wq.server.entity.Customer;
import com.wq.server.mapper.CustomerMapper;
import com.wq.server.service.CustomerService;
import com.wq.server.vo.CustomerVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Customer management implementation. Reads, writes and deletes are all executed with
 * the tenant predicate injected by the MyBatis-Plus plugin.
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<CustomerVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Customer::getName, keyword).or().like(Customer::getCompany, keyword));
        }
        wrapper.orderByDesc(Customer::getCreatedAt);
        Page<Customer> result = customerMapper.selectPage(new Page<>(page, size), wrapper);
        List<CustomerVO> records = result.getRecords().stream().map(CustomerVO::from).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public CustomerVO getById(Long id) {
        return CustomerVO.from(getOrThrow(id));
    }

    @Override
    @Transactional
    public CustomerVO create(CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setTenantId(TenantContext.getRequiredTenantId());
        customer.setName(request.name());
        customer.setCompany(request.company());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setStatus(resolveStatus(request.status()));
        customer.setRemark(request.remark());
        customerMapper.insert(customer);
        return CustomerVO.from(customer);
    }

    @Override
    @Transactional
    public CustomerVO update(Long id, CustomerUpdateRequest request) {
        Customer customer = getOrThrow(id);
        customer.setName(request.name());
        customer.setCompany(request.company());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setStatus(resolveStatus(request.status()));
        customer.setRemark(request.remark());
        customerMapper.updateById(customer);
        return CustomerVO.from(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        customerMapper.deleteById(id);
    }

    private Customer getOrThrow(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Customer not found in the current tenant");
        }
        return customer;
    }

    private String resolveStatus(String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        return DEFAULT_STATUS;
    }
}
