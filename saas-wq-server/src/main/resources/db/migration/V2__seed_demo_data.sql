-- Demo data for local evaluation. Every account uses the password Admin@123.
INSERT INTO sys_tenant (id, code, name, contact_email, status, created_at, updated_at, deleted)
VALUES (1, 'platform', 'Platform Operations', 'ops@saas-wq.dev', 'ACTIVE', NOW(), NOW(), 0),
       (2, 'acme', 'Acme Corporation', 'admin@acme.dev', 'ACTIVE', NOW(), NOW(), 0),
       (3, 'globex', 'Globex Industries', 'admin@globex.dev', 'ACTIVE', NOW(), NOW(), 0);

INSERT INTO sys_user (id, tenant_id, username, password, nickname, email, role, status, created_at, updated_at, deleted)
VALUES (1, 1, 'superadmin', '$2b$10$0Ff4eMRFphRwo07EPTCbj.yzlSyQ9vir7YprtYjOI0IIg9gDrh0.K',
        'Platform Owner', 'superadmin@saas-wq.dev', 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW(), 0),
       (2, 2, 'acme.admin', '$2b$10$0Ff4eMRFphRwo07EPTCbj.yzlSyQ9vir7YprtYjOI0IIg9gDrh0.K',
        'Acme Administrator', 'admin@acme.dev', 'TENANT_ADMIN', 'ACTIVE', NOW(), NOW(), 0),
       (3, 2, 'acme.member', '$2b$10$0Ff4eMRFphRwo07EPTCbj.yzlSyQ9vir7YprtYjOI0IIg9gDrh0.K',
        'Acme Member', 'member@acme.dev', 'MEMBER', 'ACTIVE', NOW(), NOW(), 0),
       (4, 3, 'globex.admin', '$2b$10$0Ff4eMRFphRwo07EPTCbj.yzlSyQ9vir7YprtYjOI0IIg9gDrh0.K',
        'Globex Administrator', 'admin@globex.dev', 'TENANT_ADMIN', 'ACTIVE', NOW(), NOW(), 0);

INSERT INTO biz_customer (id, tenant_id, name, company, email, phone, status, remark, created_at, updated_at, deleted)
VALUES (1, 2, 'Alice Johnson', 'Northwind Traders', 'alice@northwind.example', '+1-202-555-0101', 'ACTIVE',
        'Interested in the enterprise plan', NOW(), NOW(), 0),
       (2, 2, 'Bob Martinez', 'Contoso Ltd', 'bob@contoso.example', '+1-202-555-0102', 'ACTIVE',
        'Renewal due next quarter', NOW(), NOW(), 0),
       (3, 2, 'Carol Lee', 'Fabrikam Inc', 'carol@fabrikam.example', '+1-202-555-0103', 'INACTIVE',
        'Churned after the trial', NOW(), NOW(), 0),
       (4, 3, 'David Chen', 'Initech', 'david@initech.example', '+1-202-555-0201', 'ACTIVE',
        'Referred by an existing customer', NOW(), NOW(), 0),
       (5, 3, 'Eva Novak', 'Umbrella Group', 'eva@umbrella.example', '+1-202-555-0202', 'ACTIVE',
        'Requested a product demo', NOW(), NOW(), 0);
