# 🔌 API Endpoints Specification

Enhauthserv exposes OpenID Connect (OIDC) and OAuth 2.1 compliant endpoints. Every endpoint supports both the default root namespace and tenant-namespaced URLs.

---

## 🗺️ Endpoint Paths Map

When multi-tenancy is active, you can access endpoints via the default route or prefix them with `/t/{tenant}`.

| Endpoint Function | Standard Path | Tenant-Scoped Path | Auth Type |
|---|---|---|---|
| **OIDC Discovery Configuration** | `GET /.well-known/openid-configuration` | `GET /t/{tenant}/.well-known/openid-configuration` | None |
| **JWK Keys Set** | `GET /oauth2/jwks` | `GET /t/{tenant}/oauth2/jwks` | None |
| **User Authorization** | `GET /oauth2/authorize` | `GET /t/{tenant}/oauth2/authorize` | User Login Session |
| **Token Exchange / Minting** | `POST /oauth2/token` | `POST /t/{tenant}/oauth2/token` | Basic Auth / Client Body |
| **OIDC User Info** | `GET /userinfo` | `GET /t/{tenant}/userinfo` | Bearer Token (AccessToken) |
| **Token Introspection** | `POST /oauth2/introspect` | `POST /t/{tenant}/oauth2/introspect` | Client Basic Auth |
| **Token Revocation** | `POST /oauth2/revoke` | `POST /t/{tenant}/oauth2/revoke` | Client Basic Auth |
| **End Session (Logout)** | `GET /connect/logout` | `GET /t/{tenant}/connect/logout` | None |
| **Logged Out Landing Page** | `GET /logged-out` | `GET /logged-out` (shared) | None |

---

## 🔐 Specialized Machine Endpoint Behaviors

### 1. Token Introspection (RFC 7662)
Used by resource servers to validate incoming Bearer tokens.
- **Path**: `POST /oauth2/introspect` or `POST /t/{tenant}/oauth2/introspect`.
- **Authorization**: Client must authenticate using HTTP Basic Auth (matches its Client ID & Secret).
- **Scope Restriction**: The calling client must have the `introspection` scope registered, or the request fails with `forbidden` / `invalid_scope`.
- **Response**:
  - *Valid Token*:
    ```json
    {
      "active": true,
      "token_type": "Bearer",
      "scope": "read openid",
      "client_id": "demo-client",
      "sub": "demo-user",
      "exp": 1781239849,
      "iat": 1781239549
    }
    ```
  - *Expired/Revoked Token*:
    ```json
    {
      "active": false
    }
    ```

### 2. Token Revocation (RFC 7009)
Allows clients to invalidate their own access or refresh tokens.
- **Path**: `POST /oauth2/revoke` or `POST /t/{tenant}/oauth2/revoke`.
- **Authorization**: Client must authenticate using HTTP Basic Auth.
- **Scope Restriction**: The calling client must have the `revocation` scope.
- **Ownership Check**: A client can **only** revoke tokens it originally requested. Attempting to revoke another client's token will bypass validation but will not perform the revocation.
- **Idempotency**: The endpoint returns an HTTP `200 OK` status even if the token does not exist or has already been revoked.
- **Cascade Revocation**: If a client revokes a `refresh_token`, any active `access_token` generated from that refresh token is also revoked automatically.

---

## 🚫 Standard Error Codes

Enhauthserv implements standard OAuth2 error structures. Common responses return the appropriate HTTP Status Code along with a JSON body:

```json
{
  "error": "[error_code]",
  "error_description": "[detailed_description]"
}
```

- **`invalid_client`** (HTTP 401): Client authorization credentials failed (or Basic header is missing on token endpoints).
- **`invalid_request`** (HTTP 400): A required parameter (like `token` or `grant_type`) is missing, or a PKCE challenge is missing when required.
- **`invalid_scope`** (HTTP 400): Client requested a scope it is not authorized for, or the scope was blocked by the `client_credentials` policy.
- **`invalid_grant`** (HTTP 400): The authorization code or refresh token is expired, invalid, or has already been rotated.
- **`unsupported_grant_type`** (HTTP 400): The requested grant type is not registered/enabled on the client configuration.
- **`unauthorized_client`** (HTTP 403): The client is authenticated but lacks required scope permissions (e.g. attempting introspection without the `introspection` scope).
