# 🛡️ Token Issuance Policies

Enhauthserv overrides the default token generation logic of Spring Authorization Server to support dynamic, policy-based constraints on token lifetimes, scope authorization, and refresh token recycling.

---

## ⚙️ Configuration Properties

Token policies are mapped to the Spring environment via [TokenPolicyProperties.java](file:///Users/bangun.saputra/Bangun/Projects/enhauthserv/src/main/java/io/github/edmaputra/enhauthserv/config/TokenPolicyProperties.java) and configured inside `application.properties`:

```properties
app.token.access-token-time-to-live=5m
app.token.refresh-token-time-to-live=7d
app.token.reuse-refresh-tokens=false
app.token.client-credentials-allowed-scopes=read,write,introspection,revocation
```

| Property | Default | Description |
|---|---|---|
| `app.token.access-token-time-to-live` | `5m` | Lifetime of access tokens. Accepts Spring Duration formats (e.g. `90s`, `15m`, `2h`). |
| `app.token.refresh-token-time-to-live` | `7d` | Lifetime of refresh tokens. |
| `app.token.reuse-refresh-tokens` | `false` | Enables/disables refresh token rotation (single-use vs multi-use refresh tokens). |
| `app.token.client-credentials-allowed-scopes` | `read, write, introspection, revocation` | Set of scopes allowed to be minted in a `client_credentials` flow. |

---

## 🔒 Scope Restrictions on Client Credentials

For machine-to-machine integrations (the `client_credentials` grant type), security policies must prevent clients from obtaining excessive privileges. 

During access token encoding, the server executes programmatic scope verification:

1. The customizer parses requested scopes.
2. It compares them against a normalized, lowercase list defined in `app.token.client-credentials-allowed-scopes`.
3. If any requested scope falls outside this allowed list, the server immediately rejects the request with an `invalid_scope` error:
   ```json
   {
     "error": "invalid_scope",
     "error_description": "Scope not allowed for client_credentials grant: [scope_name]"
   }
   ```

This logic is executed by `validateClientCredentialsScopes` inside [SecurityConfig.java](file:///Users/bangun.saputra/Bangun/Projects/enhauthserv/src/main/java/io/github/edmaputra/enhauthserv/config/SecurityConfig.java).

---

## 🔄 Refresh Token Rotation (RTR)

Refresh Token Rotation is an essential security measure for public clients (and confidential clients) to prevent token replay attacks. It is controlled by `app.token.reuse-refresh-tokens`:

### Rotation Enabled (`reuse-refresh-tokens=false`)
- Every time a client calls `/oauth2/token` with `grant_type=refresh_token`, the authorization server issues:
  1. A new `access_token`
  2. A **new** `refresh_token`
- The old refresh token is marked as **invalid** and cannot be used again.
- If a client attempts to reuse the old refresh token (which could indicate a token intercept or hijack), the server returns `invalid_grant`.

### Rotation Disabled (`reuse-refresh-tokens=true`)
- When a client performs a refresh flow, the same `refresh_token` value is kept active (within its lifetime boundary) and returned back to the client.
