# Architecture

EnhAuthServ follows a **Hexagonal (Ports & Adapters)** architecture. The boundaries are enforced automatically by `ArchitectureBoundariesTests` (ArchUnit), so the layering below is not just a convention — a violating commit fails the build.

## Layers

| Layer | Package | Rule |
|---|---|---|
| Domain | `domain/` | Pure Java — no framework imports |
| Application — use cases | `application/usecase/` | Business logic; no Spring/JPA/servlet |
| Application — ports | `application/port/in`, `application/port/out` | Interfaces only (boundaries) |
| Input adapters | `adapter/in/http/`, `adapter/in/filter/` | HTTP controllers, tenant filter |
| Output adapters | `adapter/out/` | Persistence, security, tenant, token implementations |
| Config | `config/` | Spring beans; wires ports → adapters |
| Entity / Repository | `entity/`, `repository/` | JPA entities and Spring Data repos (kept separate from domain) |

## Dependency direction

```text
adapter/in  ──▶  application/port/in  ──▶  application/usecase  ──▶  application/port/out  ──▶  adapter/out
   (controllers)        (input ports)        (business logic)         (output ports)         (DB, OAuth2 services)
```

- **Controllers never touch repositories or entities** directly — only through input port interfaces.
- **Use cases never import Spring/JPA** — they depend on output ports, which adapters implement.
- All wiring lives in `UseCaseWiringConfig`; security wiring lives in `SecurityConfig`.

## Input Ports

| Port | Implemented by | Consumed by |
|---|---|---|
| `AuthorizationConsentInputPort` | `AuthorizationConsentUseCase` | `OAuth2AuthorizationConsentController` |
| `AuthorizationPolicyInputPort` | `AuthorizationPolicyUseCase` | introspect & revoke use cases |
| `IntrospectTokenInputPort` | `IntrospectTokenUseCase` | `OAuth2TokenIntrospectionController` |
| `RevokeTokenInputPort` | `RevokeTokenUseCase` | `OAuth2TokenRevocationController` |
| `UserClaimsInputPort` | `UserClaimsUseCase` | `SecurityConfig` (token & userinfo customizers) |
| `RegisteredClientBootstrapInputPort` | `DefaultRegisteredClientBootstrapUseCase` | startup seeder |

## Output Ports

| Port | Implemented by | Backing resource |
|---|---|---|
| `ClientAuthenticationPort` | `ClientAuthenticationAdapter` | HTTP Basic parsing + client repo |
| `ConsentStoragePort` | `ConsentStorageAdapter` | `OAuth2AuthorizationConsentService` |
| `CurrentTenantPort` | `TenantContextAdapter` | `TenantContext` (thread-local) |
| `RegisteredClientManagementPort` | `RegisteredClientRepositoryAdapter` | `RegisteredClientRepository` |
| `ScopeValidationPort` | `ScopeValidationAdapter` | `RegisteredClientRepository` |
| `TokenIntrospectionPort` | `TokenIntrospectionAdapter` | `TokenIntrospectionValidator` |
| `TokenRevocationPort` | `TokenRevocationAdapter` | `OAuth2AuthorizationService` |
| `UserClaimsDataPort` | `UserClaimsRepositoryAdapter` | profile/attribute/rule repositories |

## Request lifecycle

1. `TenantContextFilter` (highest precedence) resolves the tenant and stores it in `TenantContext` (thread-local).
2. One of four ordered Spring Security filter chains handles the request (machine endpoints → authorization server → introspect/revoke → default).
3. Controllers delegate to input ports; use cases orchestrate logic through output ports.
4. Tenant-aware adapters scope every OAuth2 read/write to the current tenant.
5. The filter clears `TenantContext` in a `finally` block.

See [Multi-Tenancy](03-multi-tenancy.md) for the filter-chain and tenant-resolution detail.

## Request lifecycle — sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant F as TenantContextFilter
    participant SC as SecurityFilterChain (1–4)
    participant Ctrl as Controller / SAS endpoint
    participant UC as Use case (input port)
    participant OP as Output port → adapter
    participant DB as DB / OAuth2 services

    C->>F: HTTP request
    F->>F: ResolveTenantUseCase.resolve(uri, header, ip)
    F->>F: TenantContext.setCurrentTenant(tenant)
    F->>SC: doFilter (path rewritten for machine endpoints)
    SC->>Ctrl: dispatch to matched chain
    Ctrl->>UC: call input port
    UC->>OP: call output port
    OP->>DB: read/write (scoped by TenantContext)
    DB-->>OP: rows
    OP-->>UC: domain data
    UC-->>Ctrl: result
    Ctrl-->>C: HTTP response
    Note over F: finally → TenantContext.clear()
```

## Where things live (quick map)

| Concern | Key classes |
|---|---|
| Spring + port wiring | `config/SecurityConfig`, `config/UseCaseWiringConfig`, `config/TokenPolicyProperties` |
| Tenant plumbing | `tenant/TenantContextFilter`, `tenant/TenantContext`, `tenant/TenantAware*`, `tenant/TenantIssuerService` |
| Controllers | `adapter/in/http/*Controller` |
| Use cases | `application/usecase/**` |
| Ports | `application/port/in/*`, `application/port/out/*` |
| Output adapters | `adapter/out/{persistence,security,tenant,token}/*Adapter` |
| Shared services | `service/{ClientAuthenticationService,TokenIntrospectionValidator,RevocationAuthorizationService}` |
| JPA | `entity/*`, `repository/*` |
