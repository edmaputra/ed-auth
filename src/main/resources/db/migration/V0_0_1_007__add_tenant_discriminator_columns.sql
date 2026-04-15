ALTER TABLE oauth2_registered_client
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_oauth2_registered_client_tenant
    ON oauth2_registered_client (tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_oauth2_registered_client_tenant_client
    ON oauth2_registered_client (tenant_id, client_id);

ALTER TABLE oauth2_authorization
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_oauth2_authorization_tenant
    ON oauth2_authorization (tenant_id);

ALTER TABLE oauth2_authorization_consent
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_oauth2_authorization_consent_tenant
    ON oauth2_authorization_consent (tenant_id);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_users_tenant
    ON users (tenant_id);

ALTER TABLE authorities
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_authorities_tenant
    ON authorities (tenant_id);

ALTER TABLE user_profile_attributes
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

UPDATE user_profile_attributes upa
SET tenant_id = (
    SELECT up.tenant
    FROM user_profiles up
    WHERE up.username = upa.username
)
WHERE upa.tenant_id = 'demo';

CREATE INDEX IF NOT EXISTS ix_user_profile_attributes_tenant_user
    ON user_profile_attributes (tenant_id, username);

DROP INDEX IF EXISTS ix_user_profile_attribute_key;

CREATE UNIQUE INDEX IF NOT EXISTS ix_user_profile_attribute_tenant_key
    ON user_profile_attributes (tenant_id, username, attribute_key);

ALTER TABLE claim_inclusion_rules
    ADD COLUMN IF NOT EXISTS tenant_id varchar(100) NOT NULL DEFAULT 'demo';

CREATE INDEX IF NOT EXISTS ix_claim_inclusion_rules_tenant
    ON claim_inclusion_rules (tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_claim_inclusion_rules_tenant_key
    ON claim_inclusion_rules (tenant_id, attribute_key);