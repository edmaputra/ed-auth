# Feature 3 — Multi-Tenancy

Every piece of OAuth2 state — registered clients, authorizations, consents, users, and profile attributes — is isolated per tenant. Tenant identity is resolved once per request and propagated through a thread-local context.

> **Resolution model:** the tenant is carried **in the request path** (`/t/{tenant}/...`). Header-based resolution (`X-Tenant-ID`) is **not** supported.

## Tenant resolution

`TenantContextFilter` (Spring `OncePerRequestFilter`, **highest precedence**) delegates to `ResolveTenantUseCase`, which resolves the tenant from the request path:

1. **Path** (if `path-enabled`) — pattern `/t/{tenant}/...`. For machine endpoints, the path is rewritten (e.g. `/t/{tenant}/oauth2/introspect` → `/oauth2/introspect`) so downstream chains see the canonical path.
2. **Fallback** — tenant `demo`, unless `require-explicit-tenant=true`, in which case the request is rejected with `400 Bad Request`.

Tenant IDs must match `^[A-Za-z0-9_-]+$`.

The resolved value is stored via `TenantContext.setCurrentTenant(...)` and cleared in a `finally` block.

## TenantContext

Thread-local holder:

- `setCurrentTenant(id)` / `getCurrentTenant()` → `Optional<String>`
- `getCurrentTenantOrDefault(fallback)`
- `clear()`

Use cases reach it through the `CurrentTenantPort` abstraction rather than touching the thread-local directly.

## Tenant-aware OAuth2 services

Each extends its Spring JDBC counterpart and filters/stamps `tenant_id` (default `demo`):

| Service | Base class | Behavior |
|---|---|---|
| `TenantAwareRegisteredClientRepository` | `JdbcRegisteredClientRepository` | `save` stamps tenant; `findById`/`findByClientId` filter by tenant |
| `TenantAwareOAuth2AuthorizationService` | `JdbcOAuth2AuthorizationService` | `save`/`findById`/`findByToken` scoped to tenant |
| `TenantAwareOAuth2AuthorizationConsentService` | `JdbcOAuth2AuthorizationConsentService` | `save`/`findById` scoped to tenant |

## Per-tenant issuer

`TenantIssuerService.resolveTenantIssuer(baseIssuer, tenantId)` returns `{baseIssuer}/t/{tenantId}` for a valid tenant, or the bare base issuer when no tenant is supplied. This drives discovery, JWKS, and token `iss` claims.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `tenant.resolution.path-enabled` | `true` | Allow `/t/{tenant}/` path resolution |
| `tenant.resolution.require-explicit-tenant` | `false` | Reject requests without a resolved tenant |

## Database

Migration `V0_0_1_007` adds a `tenant_id` discriminator (default `demo`) to `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`, `users`, `authorities`, `user_profile_attributes`, and `claim_inclusion_rules`, with supporting composite indexes.

## Implementation

| Concern | Class / file |
|---|---|
| Filter registration | `SecurityConfig.tenantContextFilterRegistration(...)` (`HIGHEST_PRECEDENCE`, URL `/*`) |
| Request filter | [`tenant/TenantContextFilter`](../../src/main/java/io/github/edmaputra/enhauthserv/tenant/TenantContextFilter.java) |
| Resolution logic | [`application/usecase/tenant/ResolveTenantUseCase`](../../src/main/java/io/github/edmaputra/enhauthserv/application/usecase/tenant/ResolveTenantUseCase.java) + `TenantResolutionPolicy`, `TenantResolutionResult` |
| Thread-local | [`tenant/TenantContext`](../../src/main/java/io/github/edmaputra/enhauthserv/tenant/TenantContext.java) |
| Port to use cases | `CurrentTenantPort` → [`adapter/out/tenant/TenantContextAdapter`](../../src/main/java/io/github/edmaputra/enhauthserv/adapter/out/tenant/TenantContextAdapter.java) |
| Tenant-aware stores | `tenant/TenantAwareRegisteredClientRepository`, `TenantAwareOAuth2AuthorizationService`, `TenantAwareOAuth2AuthorizationConsentService` |
| Issuer | `tenant/TenantIssuerService` |

Notes from the code:

- The filter computes a `TenantResolutionResult`; for machine endpoints it returns a **rewritten path** and forwards a `HttpServletRequestWrapper` (`MachineEndpointRewriteRequest`) so the canonical `/oauth2/introspect|revoke` matchers fire downstream.
- On `invalidRequest()` (strict mode) it writes `400 {"error":"invalid_request"}` directly and stops the chain.
- `TenantContext.clear()` always runs in `finally`.

> The `ResolveTenantUseCase` and `TenantContextFilter` constructor still accept header-resolution settings (`header-enabled`, `header-name`, …). Those become dead configuration once the [path-only change](#tenant-resolution) lands in code.

## Tenant resolution — sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant F as TenantContextFilter
    participant R as ResolveTenantUseCase
    participant Ctx as TenantContext (thread-local)
    participant Chain as Downstream filter chain

    C->>F: HTTP request (/t/{tenant}/...)
    F->>R: resolve(requestUri, header, remoteAddr)
    R-->>F: TenantResolutionResult(tenantId, source, rewrittenPath?)
    alt tenant resolved
        F->>Ctx: setCurrentTenant(tenantId)
    else strict mode + unresolved
        F-->>C: 400 invalid_request
    end
    alt machine endpoint
        F->>Chain: doFilter(rewritten request)
    else normal
        F->>Chain: doFilter(request)
    end
    Chain-->>F: response
    F->>Ctx: clear() (finally)
    F-->>C: response
```

## Related tests

- `ResolveTenantUseCaseTests`, `TenantContextFilterTests`, `TenantResolutionStrictModeTests`
