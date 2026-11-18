-- Billing extension: tenant plan entitlement, Stripe customer binding, orders and subscriptions.

ALTER TABLE sys_tenant
    ADD COLUMN plan_code       VARCHAR(32) DEFAULT NULL COMMENT 'Current subscription plan code',
    ADD COLUMN plan_expires_at DATETIME    DEFAULT NULL COMMENT 'Current subscription expiry';

ALTER TABLE biz_customer
    ADD COLUMN stripe_customer_id VARCHAR(64) DEFAULT NULL COMMENT 'Stripe customer identifier',
    ADD UNIQUE KEY uk_biz_customer_stripe (tenant_id, stripe_customer_id);

-- Payment attempts, one row per Stripe Checkout session.
CREATE TABLE IF NOT EXISTS biz_order
(
    id                         BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    tenant_id                  BIGINT      NOT NULL COMMENT 'Owning tenant',
    customer_id                BIGINT      NOT NULL COMMENT 'Billing customer',
    order_no                   VARCHAR(40) NOT NULL COMMENT 'Human readable order number',
    plan_code                  VARCHAR(32) NOT NULL COMMENT 'Purchased plan code',
    amount_cents               BIGINT      NOT NULL COMMENT 'Amount in the smallest currency unit',
    currency                   VARCHAR(8)  NOT NULL DEFAULT 'usd' COMMENT 'ISO 4217 currency code',
    status                     VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PAID or FAILED',
    stripe_checkout_session_id VARCHAR(128)         DEFAULT NULL COMMENT 'Stripe Checkout session identifier',
    stripe_subscription_id     VARCHAR(128)         DEFAULT NULL COMMENT 'Stripe subscription identifier',
    paid_at                    DATETIME             DEFAULT NULL COMMENT 'Payment confirmation time',
    created_at                 DATETIME    NOT NULL COMMENT 'Creation time',
    updated_at                 DATETIME    NOT NULL COMMENT 'Last update time',
    deleted                    TINYINT     NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_order_no (order_no),
    UNIQUE KEY uk_biz_order_checkout_session (stripe_checkout_session_id),
    UNIQUE KEY uk_biz_order_stripe_subscription (stripe_subscription_id),
    KEY idx_biz_order_tenant (tenant_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant scoped orders';

-- Current subscription of a tenant, one active record per tenant.
CREATE TABLE IF NOT EXISTS biz_subscription
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    tenant_id              BIGINT       NOT NULL COMMENT 'Owning tenant',
    customer_id            BIGINT       NOT NULL COMMENT 'Billing customer',
    plan_code              VARCHAR(32)  NOT NULL COMMENT 'Subscribed plan code',
    status                 VARCHAR(16)  NOT NULL DEFAULT 'INCOMPLETE' COMMENT 'INCOMPLETE, ACTIVE, PAST_DUE or CANCELED',
    stripe_subscription_id VARCHAR(128)          DEFAULT NULL COMMENT 'Stripe subscription identifier',
    current_period_start   DATETIME              DEFAULT NULL COMMENT 'Current billing period start',
    current_period_end     DATETIME              DEFAULT NULL COMMENT 'Current billing period end',
    canceled_at            DATETIME              DEFAULT NULL COMMENT 'Cancellation time',
    created_at             DATETIME     NOT NULL COMMENT 'Creation time',
    updated_at             DATETIME     NOT NULL COMMENT 'Last update time',
    deleted                TINYINT      NOT NULL DEFAULT 0 COMMENT 'Logic delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_subscription_tenant (tenant_id),
    UNIQUE KEY uk_biz_subscription_stripe (stripe_subscription_id),
    KEY idx_biz_subscription_customer (customer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant subscriptions';
