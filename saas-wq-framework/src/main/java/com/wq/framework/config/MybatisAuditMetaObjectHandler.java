package com.wq.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * Fills audit columns so that no service has to manage them manually.
 */
@Component
public class MybatisAuditMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATED_AT = "createdAt";

    private static final String UPDATED_AT = "updatedAt";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, CREATED_AT, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATED_AT, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName(UPDATED_AT, LocalDateTime.now(), metaObject);
    }
}
