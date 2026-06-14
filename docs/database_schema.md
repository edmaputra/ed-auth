# 🗄️ Database Schema & Persistence

Enhauthserv uses **Flyway** for database schema management and migration. By default, it runs on an in-memory **H2** database (`jdbc:h2:mem:authdb`), but its SQL schemas are compatible with standard relational databases (such as PostgreSQL or MySQL) through JDBC and JPA persistence layers.

---

## 🚦 Flyway Schema Migrations List

The schema is built incrementally through SQL scripts located under `src/main/resources/db/migration/`:

| Migration Version | Description | Target Component |
|---|---|---|
| `V0_0_1_001__authorization_server_schema.sql` | Standard Spring Authorization Server schema | OAuth2 Core Tables |
| `V0_0_1_002__user_schema.sql` | Standard Spring Security schema for login authentication | Users & Authorities |
| `V0_0_1_003__user_profile_schema.sql` | Creates user profile details table | User Profiles |
| `V0_0_1_004__user_profile_attributes_schema.sql` | Creates user custom attributes key-value table | Profile Attributes |
| `V0_0_1_005__claim_inclusion_rules_with_targets.sql` | Creates claims targeting rule mapping table | Custom Claims |
| `V0_0_1_006__drop_legacy_profile_attribute_flags.sql` | Drops legacy flag columns on attributes table | Database Cleanup |
| `V0_0_1_007__add_tenant_discriminator_columns.sql` | Adds multi-tenancy `tenant_id` columns, unique indexes, and defaults | Tenant Partitioning |

---

## 📊 Core Tables Definition

### 1. Spring Security Login Tables
Used by standard Spring Security authentication to validate end-user credentials:
- **`users`**:
  - `username` (VARCHAR(50), PK)
  - `password` (VARCHAR(500))
  - `enabled` (BOOLEAN)
  - `tenant_id` (VARCHAR(100)) - Used to group users under specific tenants.
- **`authorities`**:
  - `username` (VARCHAR(50), FK to `users`)
  - `authority` (VARCHAR(50))
  - `tenant_id` (VARCHAR(100))

### 2. Spring Authorization Server Core Tables
- **`oauth2_registered_client`**: Holds details of registered OAuth2 applications.
  - `id` (VARCHAR(100), PK)
  - `client_id` (VARCHAR(100))
  - `client_secret` (VARCHAR(200))
  - `scopes` (VARCHAR(1000))
  - `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`, `post_logout_redirect_uris`
  - `client_settings`, `token_settings`
  - `tenant_id` (VARCHAR(100)) - Unique composite index on `(tenant_id, client_id)`.
- **`oauth2_authorization`**: Stores active authorization codes, login states, access tokens, refresh tokens, and OIDC ID tokens.
  - `id` (VARCHAR(100), PK)
  - `registered_client_id` (VARCHAR(100))
  - `principal_name` (VARCHAR(200))
  - `access_token_value`, `refresh_token_value`, `oidc_id_token_value`
  - `tenant_id` (VARCHAR(100)) - Index on `(tenant_id)`.
- **`oauth2_authorization_consent`**: Keeps track of user-approved scopes for specific client applications.
  - `registered_client_id` (VARCHAR(100), PK composite)
  - `principal_name` (VARCHAR(200), PK composite)
  - `authorities` (VARCHAR(1000))
  - `tenant_id` (VARCHAR(100)) - Index on `(tenant_id)`.

### 3. Custom Claim Profiles Tables
- **`user_profiles`**: Basic profile data for users.
  - `username` (VARCHAR(50), PK, FK to `users.username`)
  - `full_name` (VARCHAR(150))
  - `email` (VARCHAR(254))
  - `email_verified` (BOOLEAN)
  - `locale` (VARCHAR(35))
  - `zoneinfo` (VARCHAR(100))
  - `department` (VARCHAR(100))
  - `tenant` (VARCHAR(100)) - Stores user's native home tenant.
- **`user_profile_attributes`**: Custom metadata dictionary for users.
  - `id` (BIGINT, PK Auto-increment)
  - `username` (VARCHAR(50), FK to `user_profiles.username`)
  - `attribute_key` (VARCHAR(120))
  - `attribute_value` (VARCHAR(1000))
  - `tenant_id` (VARCHAR(100)) - Unique composite index on `(tenant_id, username, attribute_key)`.
- **`claim_inclusion_rules`**: Tells the system where custom metadata fields belong.
  - `attribute_key` (VARCHAR(120), PK composite)
  - `tenant_id` (VARCHAR(100), PK composite)
  - `targets` (VARCHAR(300)) - e.g. `"USERINFO,ACCESS_TOKEN"`.
