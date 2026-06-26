# Architecture

EnhAuthServ follows a **Vertical-Slice Modular Monolith**. The codebase is organized around feature modules, and each module owns the classes that implement one business capability.

## Module map

| Package | Feature area | What it owns |
|---|---|---|
| `authorization/` | Authorization server policy | Authorization checks, scope validation, and authorization-code / PKCE flow rules |
| `claims/` | OpenID Connect claims | User claim assembly, claim filtering, and UserInfo claim data |
| `clients/` | Client management | Client bootstrap, client authentication, and client scope rules |
| `consent/` | User consent | Consent decisions, consent persistence, and consent approval flow |
| `tenancy/` | Multi-tenancy | Tenant resolution, tenant context, tenant issuer, and request tenant filtering |
| `tokens/` | Token lifecycle | Token policy, introspection, revocation, and token-related rules |
| `users/` | User profile data | User profile and attribute data used by claims and identity flows |
| `oauth/` | OIDC / security wiring | Spring Security config, metadata endpoints, and JWK exposure |
| `shared/` | Shared support | Cross-feature helpers such as logout handling |

## Module dependencies

The module boundaries are enforced by Spring Modulith package annotations.

| Module | Allowed dependencies |
|---|---|
| `authorization` | `clients` |
| `claims` | `users`, `tenancy` |
| `clients` | `tenancy`, `oauth` |
| `consent` | `tenancy`, `oauth` |
| `tokens` | `authorization`, `clients`, `tenancy`, `oauth` |
| `oauth` | `tenancy`, `shared` |
| `tenancy` | none |
| `shared` | none |

## Runtime flow

1. `tenancy/TenantContextFilter` resolves the tenant from the request and stores it in `TenantContext`.
2. Spring Security routes the request to the right feature entry point: authorization, token, consent, metadata, logout, or user-info.
3. The feature module applies its own business rules and collaborates with the modules it is allowed to depend on.
4. Profile and OAuth2 state are read or written through the feature packages and security services used by those modules.
5. `TenantContext` is cleared at the end of the request.

## Where things live

| Concern | Key packages / classes |
|---|---|
| Authorization flow | `authorization/**` |
| Consent flow | `consent/**` |
| Token endpoints | `tokens/**` |
| OIDC claims and userinfo | `claims/**`, `users/**` |
| Client bootstrap and auth | `clients/**` |
| Tenant resolution | `tenancy/**` |
| Metadata and security setup | `oauth/**` |
| Shared logout support | `shared/**` |

See [Multi-Tenancy](03-multi-tenancy.md) for request-resolution details.
