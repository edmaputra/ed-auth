# Feature 7 — Token Revocation (RFC 7009)

Clients can proactively invalidate an access or refresh token they were issued.

## Endpoints

| Endpoint | Method | Auth |
|---|---|---|
| `/oauth2/revoke` | POST (form) | HTTP Basic (client) |
| `/t/{tenant}/oauth2/revoke` | POST (form) | HTTP Basic (client) |

Parameters: `token` (required), `token_type_hint` (optional — `access_token` or `refresh_token`). On success returns `200 OK` with an empty body (per RFC 7009).

## API contract

`Content-Type: application/x-www-form-urlencoded`. Client authenticates with HTTP Basic and must hold the `revocation` scope.

```http
POST /oauth2/revoke HTTP/1.1
Authorization: Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=
Content-Type: application/x-www-form-urlencoded

token=8xLOxBtZp8...&token_type_hint=refresh_token
```

| Form param | Required | Description |
|---|---|---|
| `token` | yes | The token to revoke |
| `token_type_hint` | no | `access_token` or `refresh_token` |

**Success** — `200 OK`, empty body (per RFC 7009; also returned when the token is unknown).

**Error responses** (body `{ "error": ..., "error_description": ... }`):

| Status | `error` | Cause |
|---|---|---|
| `400 Bad Request` | `invalid_request` | Missing `token` parameter |
| `401 Unauthorized` | `invalid_client` | Client authentication failed |
| `403 Forbidden` | `unauthorized_client` | Client lacks the `revocation` scope |

## Flow (`RevokeTokenUseCase`)

1. Validate `token` is present → otherwise `400 invalid_request`.
2. Authenticate the client via HTTP Basic (`ClientAuthenticationPort`) → otherwise `401`.
3. Confirm the client holds the `revocation` scope (`AuthorizationPolicyInputPort`) → otherwise `403 unauthorized_client`.
4. Delegate to `TokenRevocationPort`, which invalidates the token for the authenticated client via the (tenant-aware) `OAuth2AuthorizationService`.

A client may only revoke tokens it owns; revocation is scoped to the authenticated `registeredClientId`.

## Implementation

| Concern | Class / file |
|---|---|
| Controller | [`adapter/in/http/OAuth2TokenRevocationController`](../../src/main/java/io/github/edmaputra/enhauthserv/adapter/in/http/OAuth2TokenRevocationController.java) |
| Input port + use case | `RevokeTokenInputPort` → [`RevokeTokenUseCase`](../../src/main/java/io/github/edmaputra/enhauthserv/application/usecase/revocation/RevokeTokenUseCase.java) (+ `RevokeTokenCommand`, `RevokeTokenResult`) |
| Client auth | `ClientAuthenticationPort` → `adapter/out/security/ClientAuthenticationAdapter` → `service/ClientAuthenticationService` |
| Scope policy | `AuthorizationPolicyInputPort` → `AuthorizationPolicyUseCase` → `ScopeValidationPort` → `ScopeValidationAdapter` |
| Revocation | `TokenRevocationPort` → `adapter/out/token/TokenRevocationAdapter` (+ `service/RevocationAuthorizationService`) → `TenantAwareOAuth2AuthorizationService` |
| Filter chains | `SecurityConfig` `@Order(1)` (tenant path) and `@Order(3)` (base path), both permitAll |

Notes from the code:

- Same `permitAll` + in-use-case auth pattern as introspection; the scope gate here is the `revocation` scope.
- Revocation is bound to the authenticated `registeredClientId`, so a client can only revoke tokens it owns. An unknown token still returns `200 OK`.

## Revocation — sequence

```mermaid
sequenceDiagram
    participant Cl as Client
    participant Ctrl as OAuth2TokenRevocationController
    participant UC as RevokeTokenUseCase
    participant Auth as ClientAuthenticationPort
    participant Pol as AuthorizationPolicyInputPort
    participant Rev as TokenRevocationPort
    participant Store as TenantAwareOAuth2AuthorizationService

    Cl->>Ctrl: POST /oauth2/revoke (Basic auth, token, hint?)
    Ctrl->>UC: revoke(RevokeTokenCommand)
    alt token missing
        UC-->>Ctrl: 400 invalid_request
    end
    UC->>Auth: authenticateBasic(header)
    alt not authenticated
        UC-->>Ctrl: 401 invalid_client
    end
    UC->>Pol: validateScope(clientId, "revocation")
    alt scope missing
        UC-->>Ctrl: 403 unauthorized_client
    end
    UC->>Rev: revokeTokenForClient(token, hint, registeredClientId)
    Rev->>Store: invalidate token for client
    Store-->>Rev: done
    Rev-->>UC: ok
    UC-->>Ctrl: 200 OK (empty)
    Ctrl-->>Cl: 200 OK
```

## Related tests

- `TokenRevocationEndpointTests`, `RevokeTokenUseCaseTests`
