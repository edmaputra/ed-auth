# EnhAuthServ — Documentation

**EnhAuthServ** is a multi-tenant **Identity Provider (IdP)** built on Spring Authorization Server. It issues and manages OAuth 2.0 / OpenID Connect tokens, isolates state per tenant, and supports dynamically-assembled identity claims.

This documentation set describes **what the system does today** (Features) and **how the code is organized** (Architecture and modules).

## How this is organized

| Section | Description |
|---|---|
| [Architecture](features/00-architecture.md) | Vertical-slice module map, package boundaries, request lifecycle |
| **Features** | One file per capability — see table below |
| [Roadmap](roadmap/README.md) | Planned features to cover the full IdP problem space |

## Current code structure

EnhAuthServ is organized as a **Vertical-Slice Modular Monolith**. The main business capabilities are packaged as Spring Modulith application modules, with supporting technical layers kept beside them.

| Concern | Packages | Notes |
|---|---|---|
| Vertical slices / application modules | `authorization/**`, `claims/**`, `clients/**`, `consent/**`, `tenancy/**`, `tokens/**`, `users/**` | Feature-oriented modules with explicit module boundaries |
| OAuth / security wiring | `oauth/**`, `shared/**` | Spring Security configuration, metadata, and shared support |
| Application use cases | `application/usecase/**` | Use-case classes, commands, and module-facing application logic |
| Input adapters | `adapter/in/http/**`, `adapter/in/filter/**` | HTTP controllers and request filtering |
| Output adapters | `adapter/out/persistence/**`, `adapter/out/security/**` | Persistence and security implementations |
| Persistence model | `entity/**`, `repository/**` | JPA entities and Spring Data repositories, kept separate from modules |
| Spring configuration | `config/**` | Property binding and wiring helpers |

See [Architecture](features/00-architecture.md) for the full module map and dependency rules.

## Feature Index

| # | Feature | File |
|---|---|---|
| 1 | OAuth 2.0 Authorization Server | [features/01-oauth2-authorization-server.md](features/01-oauth2-authorization-server.md) |
| 2 | OpenID Connect (OIDC) | [features/02-openid-connect.md](features/02-openid-connect.md) |
| 3 | Multi-Tenancy | [features/03-multi-tenancy.md](features/03-multi-tenancy.md) |
| 4 | Token Policy & Lifetime Controls | [features/04-token-policy.md](features/04-token-policy.md) |
| 5 | Dynamic Claims | [features/05-dynamic-claims.md](features/05-dynamic-claims.md) |
| 6 | Token Introspection (RFC 7662) | [features/06-token-introspection.md](features/06-token-introspection.md) |
| 7 | Token Revocation (RFC 7009) | [features/07-token-revocation.md](features/07-token-revocation.md) |
| 8 | User Consent | [features/08-consent.md](features/08-consent.md) |
| 9 | Client Management & Bootstrap | [features/09-client-management.md](features/09-client-management.md) |
| 10 | Session & Logout | [features/10-session-logout.md](features/10-session-logout.md) |
| 11 | Persistence & Schema | [features/11-persistence-schema.md](features/11-persistence-schema.md) |
| 12 | Configuration Reference | [features/12-configuration.md](features/12-configuration.md) |

## Quick Start

```bash
./mvnw clean package          # Build
./mvnw spring-boot:run        # Run (port 9000)
./mvnw test                   # All tests
```

- H2 console: `http://localhost:9000/h2-console` (JDBC URL `jdbc:h2:mem:authdb`, user `sa`)
- OIDC discovery: `GET http://localhost:9000/t/demo/.well-known/openid-configuration`

### Built-in credentials (dev only)

| Type | Client / User | Secret |
|---|---|---|
| Confidential client | `demo-client` | `demo-secret` |
| Public client (PKCE) | `pkce-public-client` | — |
| End user | `demo-user` | `demo-password` |

## Technology Stack

- Spring Boot 3.x / Spring Security 6.x (OAuth2 Authorization Server)
- Spring Data JPA + Flyway
- H2 (default; swappable for any JDBC database)
- Nimbus JOSE (JWT/JWK)
