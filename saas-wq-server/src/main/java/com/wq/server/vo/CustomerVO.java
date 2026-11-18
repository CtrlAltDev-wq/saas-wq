package com.wq.server.vo;

import com.wq.server.entity.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Customer representation returned to API consumers.
 *
 * @param id               customer identifier
 * @param name             contact name
 * @param company          company name
 * @param email            contact mailbox
 * @param phone            contact phone number
 * @param stripeCustomerId Stripe customer identifier, null until the first checkout
 * @param status           ACTIVE or INACTIVE
 * @param remark           free-form notes
 * @param createdAt        creation time
 */
@Schema(description = "Customer profile")
public record CustomerVO(Long id, String name, String company, String email, String phone, String stripeCustomerId,
                         String status, String remark, LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to its API representation.
     *
     * @param customer entity
     * @return customer view object
     */
    public static CustomerVO from(Customer customer) {
        return new CustomerVO(customer.getId(), customer.getName(), customer.getCompany(), customer.getEmail(),
                customer.getPhone(), customer.getStripeCustomerId(), customer.getStatus(), customer.getRemark(),
                customer.getCreatedAt());
    }
}
