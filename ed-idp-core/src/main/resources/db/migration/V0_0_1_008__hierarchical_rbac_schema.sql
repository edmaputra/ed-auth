-- V0_0_1_008__hierarchical_rbac_schema.sql
-- Hierarchical Role-Based Access Control (RBAC): Permissions, Roles, Groups, and User Mappings

-- 1. Permissions (fine-grained system privileges)
CREATE TABLE IF NOT EXISTS iam_permissions (
    id varchar(100) NOT NULL,
    code varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    description varchar(512),
    category varchar(64) NOT NULL DEFAULT 'GENERAL',
    is_system_permission boolean NOT NULL DEFAULT false,
    tenant_id varchar(100),
    created_at bigint NOT NULL DEFAULT 0,
    updated_at bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS code varchar(100);
ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS category varchar(64) DEFAULT 'GENERAL';
ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS is_system_permission boolean DEFAULT false;
ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS tenant_id varchar(100);
ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS created_at bigint DEFAULT 0;
ALTER TABLE iam_permissions ADD COLUMN IF NOT EXISTS updated_at bigint DEFAULT 0;

UPDATE iam_permissions SET code = name WHERE code IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_iam_permissions_code
    ON iam_permissions (code);

CREATE INDEX IF NOT EXISTS ix_iam_permissions_tenant
    ON iam_permissions (tenant_id);

CREATE INDEX IF NOT EXISTS ix_iam_permissions_category
    ON iam_permissions (category);

-- 2. Roles (tenant-scoped collections of permissions)
CREATE TABLE IF NOT EXISTS iam_roles (
    id varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(255),
    tenant_id varchar(100) NOT NULL DEFAULT 'demo',
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_iam_roles_tenant
    ON iam_roles (tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_iam_roles_tenant_name
    ON iam_roles (tenant_id, name);

-- 3. Role-Permission mappings
CREATE TABLE IF NOT EXISTS iam_role_permissions (
    role_id varchar(100) NOT NULL,
    permission_id varchar(100) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES iam_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_perm
        FOREIGN KEY (permission_id) REFERENCES iam_permissions (id) ON DELETE CASCADE
);

-- 4. Groups (tenant-scoped logical collections of users)
CREATE TABLE IF NOT EXISTS iam_groups (
    id varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(255),
    tenant_id varchar(100) NOT NULL DEFAULT 'demo',
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_iam_groups_tenant
    ON iam_groups (tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_iam_groups_tenant_name
    ON iam_groups (tenant_id, name);

-- 5. Group-Role mappings
CREATE TABLE IF NOT EXISTS iam_group_roles (
    group_id varchar(100) NOT NULL,
    role_id varchar(100) NOT NULL,
    PRIMARY KEY (group_id, role_id),
    CONSTRAINT fk_group_roles_group
        FOREIGN KEY (group_id) REFERENCES iam_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_roles_role
        FOREIGN KEY (role_id) REFERENCES iam_roles (id) ON DELETE CASCADE
);

-- 6. Group Members (users belonging to a group)
CREATE TABLE IF NOT EXISTS iam_group_members (
    group_id varchar(100) NOT NULL,
    username varchar(50) NOT NULL,
    tenant_id varchar(100) NOT NULL DEFAULT 'demo',
    PRIMARY KEY (group_id, username),
    CONSTRAINT fk_group_members_group
        FOREIGN KEY (group_id) REFERENCES iam_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user
        FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_iam_group_members_tenant_user
    ON iam_group_members (tenant_id, username);

-- 7. Direct User-Role mappings
CREATE TABLE IF NOT EXISTS iam_user_roles (
    username varchar(50) NOT NULL,
    role_id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL DEFAULT 'demo',
    PRIMARY KEY (username, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES iam_roles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_iam_user_roles_tenant_user
    ON iam_user_roles (tenant_id, username);

-- 8. Seed baseline permissions
INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-clients-read', 'clients:read', 'View OAuth2 Clients', 'View registered OAuth2 clients and their configuration', 'CLIENTS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-clients-read');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-clients-write', 'clients:write', 'Manage OAuth2 Clients', 'Create, update and delete OAuth2 clients', 'CLIENTS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-clients-write');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-users-read', 'users:read', 'View Users Directory', 'View user directory and profiles', 'USERS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-users-read');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-users-write', 'users:write', 'Manage User Accounts', 'Create, update and manage user accounts', 'USERS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-users-write');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-roles-manage', 'roles:manage', 'Manage RBAC Roles', 'Manage roles and permission assignments', 'RBAC', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-roles-manage');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-groups-manage', 'groups:manage', 'Manage User Groups', 'Manage user groups and memberships', 'RBAC', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-groups-manage');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-tokens-revoke', 'tokens:revoke', 'Revoke Issued Tokens', 'Revoke issued OAuth2 tokens', 'TOKENS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-tokens-revoke');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-tokens-introspect', 'tokens:introspect', 'Introspect Tokens', 'Introspect OAuth2 tokens', 'TOKENS', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-tokens-introspect');

INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
SELECT 'perm-permissions-manage', 'permissions:manage', 'Manage Permissions Catalog', 'Create, update, and manage permissions in catalog', 'RBAC', true, NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE id = 'perm-permissions-manage');

-- 9. Seed default roles for demo tenant
INSERT INTO iam_roles (id, name, description, tenant_id)
SELECT 'role-admin', 'ROLE_ADMIN', 'System Administrator with full access', 'demo'
WHERE NOT EXISTS (SELECT 1 FROM iam_roles WHERE id = 'role-admin');

INSERT INTO iam_roles (id, name, description, tenant_id)
SELECT 'role-user', 'ROLE_USER', 'Standard user with basic access', 'demo'
WHERE NOT EXISTS (SELECT 1 FROM iam_roles WHERE id = 'role-user');

-- 10. Link permissions to default roles
INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-clients-read'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-clients-read');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-clients-write'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-clients-write');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-users-read'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-users-read');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-users-write'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-users-write');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-roles-manage'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-roles-manage');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-groups-manage'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-groups-manage');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-tokens-revoke'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-tokens-revoke');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-tokens-introspect'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-tokens-introspect');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-admin', 'perm-permissions-manage'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-admin' AND permission_id = 'perm-permissions-manage');

INSERT INTO iam_role_permissions (role_id, permission_id)
SELECT 'role-user', 'perm-tokens-introspect'
WHERE NOT EXISTS (SELECT 1 FROM iam_role_permissions WHERE role_id = 'role-user' AND permission_id = 'perm-tokens-introspect');

-- 11. Seed default group
INSERT INTO iam_groups (id, name, description, tenant_id)
SELECT 'group-engineering', 'Engineering', 'Engineering organization team', 'demo'
WHERE NOT EXISTS (SELECT 1 FROM iam_groups WHERE id = 'group-engineering');

INSERT INTO iam_group_roles (group_id, role_id)
SELECT 'group-engineering', 'role-user'
WHERE NOT EXISTS (SELECT 1 FROM iam_group_roles WHERE group_id = 'group-engineering' AND role_id = 'role-user');
