CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'USER_READ', 'View user profiles'),
    (gen_random_uuid(), 'USER_CREATE', 'Create user accounts'),
    (gen_random_uuid(), 'USER_UPDATE', 'Update user accounts'),
    (gen_random_uuid(), 'USER_DELETE', 'Delete user accounts'),
    (gen_random_uuid(), 'ROLE_READ', 'View roles and permissions'),
    (gen_random_uuid(), 'ROLE_CREATE', 'Create roles'),
    (gen_random_uuid(), 'ROLE_UPDATE', 'Update roles'),
    (gen_random_uuid(), 'ROLE_DELETE', 'Delete roles'),
    (gen_random_uuid(), 'ROLE_ASSIGN', 'Assign or remove roles from users'),
    (gen_random_uuid(), 'AUDIT_READ', 'View audit logs'),
    (gen_random_uuid(), 'AUDIT_EXPORT', 'Export audit logs'),
    (gen_random_uuid(), 'API_KEY_CREATE', 'Create API keys'),
    (gen_random_uuid(), 'API_KEY_REVOKE', 'Revoke API keys'),
    (gen_random_uuid(), 'PROFILE_READ', 'View own profile'),
    (gen_random_uuid(), 'PROFILE_UPDATE', 'Update own profile'),
    (gen_random_uuid(), 'SECURITY_MANAGE', 'Manage MFA, devices and risk settings platform-wide'),
    (gen_random_uuid(), 'SESSION_MANAGE', 'View and revoke sessions platform-wide'),
    (gen_random_uuid(), 'DEVICE_MANAGE', 'View and block devices platform-wide');

INSERT INTO roles (id, name, description, system_role) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Full platform access', true),
    (gen_random_uuid(), 'SECURITY_ADMIN', 'Manages security posture: audits, devices, sessions, API keys', true),
    (gen_random_uuid(), 'ADMIN', 'Manages users and roles', true),
    (gen_random_uuid(), 'MANAGER', 'Read-only oversight of users and audit history', true),
    (gen_random_uuid(), 'USER', 'Standard authenticated end user', true),
    (gen_random_uuid(), 'READ_ONLY', 'Read-only access across the platform', true);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.name IN ('AUDIT_READ', 'AUDIT_EXPORT', 'USER_READ', 'API_KEY_REVOKE',
                  'SECURITY_MANAGE', 'SESSION_MANAGE', 'DEVICE_MANAGE')
WHERE r.name = 'SECURITY_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.name IN ('USER_READ', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE',
                  'ROLE_READ', 'ROLE_ASSIGN', 'AUDIT_READ')
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.name IN ('USER_READ', 'AUDIT_READ', 'PROFILE_READ')
WHERE r.name = 'MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.name IN ('PROFILE_READ', 'PROFILE_UPDATE')
WHERE r.name = 'USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.name IN ('USER_READ', 'AUDIT_READ', 'PROFILE_READ')
WHERE r.name = 'READ_ONLY';
