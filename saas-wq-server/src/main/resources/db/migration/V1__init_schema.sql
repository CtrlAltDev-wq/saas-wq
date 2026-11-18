-- Tenant registry: global by design and excluded from row level filtering.
CREATE TABLE IF NOT EXISTS sys_tenant
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    code          VARCHAR(64)  NOT NULL COMMENT 'Unique code used together with the username at login',
    name          VARCHAR(128) NOT NULL COMMENT 'Display name',
    contact_email VARCHAR(128) DEFAULT NULL COMMENT 'Administrator contact mailbox',
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE or DISABLED',
    created_at    DATETIME     NOT NULL COMMENT 'Creation time',
    updated_at    DATETIME     NOT NULL COMMENT 'Last update time',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_tenant_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant registry';

-- Tenant scoped user accounts.
CREATE TABLE IF NOT EXISTS sys_user
(
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    tenant_id  BIGINT      NOT NULL COMMENT 'Owning tenant',
    username   VARCHAR(64) NOT NULL COMMENT 'Login name, unique inside a tenant',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    nickname   VARCHAR(64)  DEFAULT NULL COMMENT 'Display name',
    email      VARCHAR(128) DEFAULT NULL COMMENT 'Contact mailbox',
    role       VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT 'SUPER_ADMIN, TENANT_ADMIN or MEMBER',
    status     VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE or DISABLED',
    created_at DATETIME    NOT NULL COMMENT 'Creation time',
    updated_at DATETIME    NOT NULL COMMENT 'Last update time',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_tenant_username (tenant_id, username),
    KEY idx_sys_user_tenant (tenant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant scoped users';

-- Tenant scoped customers.
CREATE TABLE IF NOT EXISTS biz_customer
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    tenant_id  BIGINT       NOT NULL COMMENT 'Owning tenant',
    name       VARCHAR(128) NOT NULL COMMENT 'Contact name',
    company    VARCHAR(128) DEFAULT NULL COMMENT 'Company name',
    email      VARCHAR(128) DEFAULT NULL COMMENT 'Contact mailbox',
    phone      VARCHAR(32)  DEFAULT NULL COMMENT 'Contact phone number',
    status     VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE or INACTIVE',
    remark     VARCHAR(512) DEFAULT NULL COMMENT 'Free-form notes',
    created_at DATETIME     NOT NULL COMMENT 'Creation time',
    updated_at DATETIME     NOT NULL COMMENT 'Last update time',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    KEY idx_biz_customer_tenant (tenant_id),
    KEY idx_biz_customer_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant scoped customers';
