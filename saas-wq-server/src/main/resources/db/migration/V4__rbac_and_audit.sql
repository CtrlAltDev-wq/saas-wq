-- Role based access control registry and tenant scoped audit trail.

-- Role catalogue. Global by design: every tenant shares the same role definitions.
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    code        VARCHAR(32) NOT NULL COMMENT 'Stable role code referenced by sys_user.role',
    name        VARCHAR(64) NOT NULL COMMENT 'Display name',
    description VARCHAR(255) DEFAULT NULL COMMENT 'Human readable purpose',
    built_in    TINYINT     NOT NULL DEFAULT 0 COMMENT 'Built-in roles cannot be renamed or removed',
    created_at  DATETIME    NOT NULL COMMENT 'Creation time',
    updated_at  DATETIME    NOT NULL COMMENT 'Last update time',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Role catalogue';

-- Permission catalogue. Codes follow the resource:action convention.
CREATE TABLE IF NOT EXISTS sys_permission
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    code        VARCHAR(64)  NOT NULL COMMENT 'Permission code, for example customer:read',
    name        VARCHAR(128) NOT NULL COMMENT 'Display name',
    resource    VARCHAR(64)  NOT NULL COMMENT 'Protected resource',
    action      VARCHAR(32)  NOT NULL COMMENT 'Allowed action',
    description VARCHAR(255) DEFAULT NULL COMMENT 'Human readable purpose',
    created_at  DATETIME     NOT NULL COMMENT 'Creation time',
    updated_at  DATETIME     NOT NULL COMMENT 'Last update time',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (code),
    KEY idx_sys_permission_resource (resource)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Permission catalogue';

-- Role to permission mapping. Global by design, edited through the RBAC API.
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    role_id       BIGINT   NOT NULL COMMENT 'Role identifier',
    permission_id BIGINT   NOT NULL COMMENT 'Permission identifier',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
    KEY idx_sys_role_permission_permission (permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Role permission mapping';

-- Audit trail. Tenant scoped: administrators only see the operations of their own tenant.
CREATE TABLE IF NOT EXISTS sys_audit_log
(
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    tenant_id     BIGINT      NOT NULL COMMENT 'Owning tenant',
    user_id       BIGINT       DEFAULT NULL COMMENT 'Actor identifier, null for failed logins',
    username      VARCHAR(64)  DEFAULT NULL COMMENT 'Actor login name',
    action        VARCHAR(64) NOT NULL COMMENT 'Business action, for example CUSTOMER_CREATE',
    resource      VARCHAR(64) NOT NULL COMMENT 'Affected resource',
    http_method   VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP method',
    uri           VARCHAR(255) DEFAULT NULL COMMENT 'Request URI',
    request_args  TEXT COMMENT 'Sanitised and truncated request arguments',
    success       TINYINT     NOT NULL DEFAULT 1 COMMENT '1 when the operation succeeded',
    error_message VARCHAR(512) DEFAULT NULL COMMENT 'Error message of a failed operation',
    duration_ms   BIGINT      NOT NULL DEFAULT 0 COMMENT 'Execution time in milliseconds',
    ip_address    VARCHAR(64)  DEFAULT NULL COMMENT 'Client address',
    created_at    DATETIME    NOT NULL COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_sys_audit_tenant_created (tenant_id, created_at),
    KEY idx_sys_audit_tenant_action (tenant_id, action),
    KEY idx_sys_audit_tenant_user (tenant_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant scoped audit trail';

-- Role seeds.
INSERT INTO sys_role (id, code, name, description, built_in, created_at, updated_at, deleted)
VALUES (1, 'SUPER_ADMIN', 'Super Administrator', 'Platform operator allowed to manage tenants and roles', 1, NOW(), NOW(), 0),
       (2, 'TENANT_ADMIN', 'Tenant Administrator', 'Administrator of a single tenant', 1, NOW(), NOW(), 0),
       (3, 'MEMBER', 'Member', 'Regular member of a tenant', 1, NOW(), NOW(), 0);

-- Permission seeds.
INSERT INTO sys_permission (id, code, name, resource, action, description, created_at, updated_at, deleted)
VALUES (1, 'tenant:read', 'Read tenants', 'tenant', 'read', 'Browse the platform tenant registry', NOW(), NOW(), 0),
       (2, 'tenant:write', 'Manage tenants', 'tenant', 'write', 'Create, update and delete tenants', NOW(), NOW(), 0),
       (3, 'user:read', 'Read users', 'user', 'read', 'Browse the users of the current tenant', NOW(), NOW(), 0),
       (4, 'user:write', 'Manage users', 'user', 'write', 'Create, update and delete users of the current tenant', NOW(), NOW(), 0),
       (5, 'customer:read', 'Read customers', 'customer', 'read', 'Browse the customers of the current tenant', NOW(), NOW(), 0),
       (6, 'customer:write', 'Manage customers', 'customer', 'write', 'Create, update and delete customers of the current tenant', NOW(), NOW(), 0),
       (7, 'order:read', 'Read orders', 'order', 'read', 'Browse the orders of the current tenant', NOW(), NOW(), 0),
       (8, 'order:write', 'Manage orders', 'order', 'write', 'Start a Stripe Checkout subscription session', NOW(), NOW(), 0),
       (9, 'subscription:read', 'Read subscription', 'subscription', 'read', 'Read the current subscription entitlement', NOW(), NOW(), 0),
       (10, 'audit:read', 'Read audit trail', 'audit', 'read', 'Browse the audit trail of the current tenant', NOW(), NOW(), 0),
       (11, 'rbac:read', 'Read roles and permissions', 'rbac', 'read', 'Browse roles and permission codes', NOW(), NOW(), 0),
       (12, 'rbac:write', 'Manage role permissions', 'rbac', 'write', 'Replace the permission set of a role', NOW(), NOW(), 0);

-- SUPER_ADMIN owns every permission.
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12);

-- TENANT_ADMIN owns everything except the platform tenant registry and role editing.
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11);

-- MEMBER is read only.
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES (3, 5), (3, 7), (3, 9);
