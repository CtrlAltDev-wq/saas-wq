package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer record owned by a tenant.
 */
@Getter
@Setter
@TableName("biz_customer")
public class Customer extends BaseEntity {

    /**
     * Owning tenant, enforced by the row level isolation plugin.
     */
    private Long tenantId;

    /**
     * Contact name.
     */
    private String name;

    /**
     * Company name.
     */
    private String company;

    /**
     * Contact mailbox.
     */
    private String email;

    /**
     * Contact phone number.
     */
    private String phone;

    /**
     * Account status, for example ACTIVE or INACTIVE.
     */
    private String status;

    /**
     * Free-form notes.
     */
    private String remark;
}
