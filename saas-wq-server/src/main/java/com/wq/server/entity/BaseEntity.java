package com.wq.server.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Common audit columns shared by every persistent entity.
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /**
     * Database generated primary key.
     */
    @TableId
    private Long id;

    /**
     * Creation time, filled by the audit handler.
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Last update time, filled by the audit handler.
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Logic delete flag; {@code 0} means alive, {@code 1} means deleted.
     */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
