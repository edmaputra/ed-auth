# Onboarding Guide — EnhAuthServ

Welcome 👋 This is your **guided learning path** for getting productive in EnhAuthServ.

EnhAuthServ is a **multi-tenant OAuth 2.1 / OpenID Connect Identity Provider** built on Spring
Authorization Server, using a strict **Hexagonal (Ports & Adapters)** architecture.

> This file is the *map*. It doesn't re-explain everything — it tells you **what to read, in what
> order, and which code to open** so knowledge builds up layer by layer. Reference material already
> lives in [documentation/](documentation/) and [docs/](docs/); this guide routes you through it.

**Suggested pace:** ~3–4 focused days. Don't skip the "Do it yourself" checkpoints — running things
beats reading about them. Tick the boxes as you go.

---

## Stage 0 — Orientation & first run (≈ half a day)

**Goal:** the app runs on your machine and you can issue one token.

1. Read the top-level [README.md](README.md) — the elevator pitch and feature list.
2. Read the project rules of the road: [CLAUDE.md](CLAUDE.md) — build commands, architecture table,
   key concepts, **built-in test credentials**, and testing gotchas. This is the single densest page; reread it later.
3. Build and run:
   ```bash
   ./mvnw clean package          # Build
   ./mvnw spring-boot:run        # Run on port 9000
   ```
4. Poke around:
   - H2 console → `http://localhost:9000/h2-console` (JDBC `jdbc:h2:mem:authdb`, user `sa`)
   - OIDC discovery → `GET http://localhost:9000/t/demo/.well-known/openid-configuration`

✅ **Checkpoint:** Run the `client_credentials` flow against `POST /oauth2/token` using
`demo-client` / `demo-secret` and receive an access token. (Credentials table is in [CLAUDE.md](CLAUDE.md).)

---

## Stage 1 — The mental model: Hexagonal architecture (≈ half a day)

**Goal:** know which layer a given class belongs to and why — this is the #1 thing that makes the
codebase navigable.

1. Read [documentation/features/00-architecture.md](documentation/features/00-architecture.md) — layering, package map, request lifecycle.
2. Read [docs/architecture.md](docs/architecture.md) — the Ports & Adapters rules with the dependency diagram.
3. Open the wiring that glues it all together: [config/UseCaseWiringConfig.java](src/main/java/io/github/edmaputra/enhauthserv/config/UseCaseWiringConfig.java).
   Trace how an **input port** (use case) gets an **output port** (adapter) injected.
4. Read the architecture *contract* the build enforces: [test/.../ArchitectureBoundariesTests.java](src/test/java/io/github/edmaputra/enhauthserv/ArchitectureBoundariesTests.java).
   These ArchUnit rules will fail your build if you cross a layer — read them to learn the boundaries.

**The layer cheat-sheet** (memorize this shape):

| Layer | Package | Rule |
|---|---|---|
| Domain | `domain/` | Pure Java, no frameworks |
| Application | `application/usecase/`, `application/port/` | No Spring/JPA/servlet |
| Input adapters | `adapter/in/http/`, `adapter/in/filter/` | Controllers, filters |
| Output adapters | `adapter/out/` | Persistence, security, tenant, token |
| Config | `config/` | Spring beans wire ports → adapters |
| Entity / Repository | `entity/`, `repository/` | JPA, kept separate from domain |

✅ **Checkpoint:** Given a class name, say which layer it's in and what it's allowed to depend on.
Controllers never touch repositories — only input ports. Confirm you understand *why*.

---

## Stage 2 — The core: how a token actually gets issued (≈ 1 day)

**Goal:** follow a full Authorization Code + PKCE flow end-to-end and understand Spring
Authorization Server's role vs. our custom code.

1. [documentation/features/01-oauth2-authorization-server.md](documentation/features/01-oauth2-authorization-server.md) — the OAuth 2.0 server core.
2. [documentation/features/02-openid-connect.md](documentation/features/02-openid-connect.md) — the OIDC layer on top.
3. [docs/api_endpoints.md](docs/api_endpoints.md) — the full endpoint catalog and auth rules.
4. Open the security wiring — this is where Spring Authorization Server is configured and where our
   custom endpoints/services hook in: [config/SecurityConfig.java](src/main/java/io/github/edmaputra/enhauthserv/config/SecurityConfig.java).
5. Walk the integration tests as living documentation of the flows:
   - [AuthServerAuthorizationFlowTests](src/test/java/io/github/edmaputra/enhauthserv/AuthServerAuthorizationFlowTests.java)
   - [AuthServerPkceFlowTests](src/test/java/io/github/edmaputra/enhauthserv/AuthServerPkceFlowTests.java)
   - Their shared base: [AuthServerIntegrationTests](src/test/java/io/github/edmaputra/enhauthserv/AuthServerIntegrationTests.java) (OAuth helpers, CSRF extraction).

✅ **Checkpoint:** Explain the difference between what Spring Authorization Server gives us
out-of-the-box and what EnhAuthServ adds. Run `./mvnw -Dtest=AuthServerPkceFlowTests test` green.

---

## Stage 3 — What makes EnhAuthServ "enhanced" (≈ 1 day)

These three features are the project's reason to exist. Read the doc, then open the named class.

### 3a. Multi-tenancy
- Read [documentation/features/03-multi-tenancy.md](documentation/features/03-multi-tenancy.md) and [docs/multitenancy.md](docs/multitenancy.md).
- Tenant resolution happens per-request: header `X-Tenant-ID` → path prefix `/t/{tenant}/`.
- Open in order:
  [tenant/TenantContextFilter.java](src/main/java/io/github/edmaputra/enhauthserv/tenant/TenantContextFilter.java) →
  [tenant/TenantContext.java](src/main/java/io/github/edmaputra/enhauthserv/tenant/TenantContext.java) (thread-local) →
  [usecase/tenant/ResolveTenantUseCase.java](src/main/java/io/github/edmaputra/enhauthserv/application/usecase/tenant/ResolveTenantUseCase.java).
- Then the tenant-scoped OAuth2 services: `TenantAwareRegisteredClientRepository`,
  `TenantAwareOAuth2AuthorizationService`, `TenantAwareOAuth2AuthorizationConsentService`,
  [TenantIssuerService.java](src/main/java/io/github/edmaputra/enhauthserv/tenant/TenantIssuerService.java) — all in [tenant/](src/main/java/io/github/edmaputra/enhauthserv/tenant/).

### 3b. Token policy & lifetimes
- Read [documentation/features/04-token-policy.md](documentation/features/04-token-policy.md) and [docs/token_policies.md](docs/token_policies.md).
- Configured via `app.token.*`: [config/TokenPolicyProperties.java](src/main/java/io/github/edmaputra/enhauthserv/config/TokenPolicyProperties.java).
- See it exercised in [TokenPolicyControlsTests](src/test/java/io/github/edmaputra/enhauthserv/TokenPolicyControlsTests.java).

### 3c. Dynamic claims
- Read [documentation/features/05-dynamic-claims.md](documentation/features/05-dynamic-claims.md) and [docs/custom_claims.md](docs/custom_claims.md).
- Claims are assembled from `UserProfile` + `UserProfileAttribute` + `ClaimInclusionRule`, routed to
  `USERINFO` / `ID_TOKEN` / `ACCESS_TOKEN` via [entity/ClaimTarget.java](src/main/java/io/github/edmaputra/enhauthserv/entity/ClaimTarget.java).
- Core logic: [usecase/claims/UserClaimsUseCase.java](src/main/java/io/github/edmaputra/enhauthserv/application/usecase/claims/UserClaimsUseCase.java),
  backed by [adapter/out/persistence/UserClaimsRepositoryAdapter.java](src/main/java/io/github/edmaputra/enhauthserv/adapter/out/persistence/UserClaimsRepositoryAdapter.java).

✅ **Checkpoint:** Add a fake profile attribute and configure a rule so it appears in the ID token
(or trace how `UserClaimsUseCaseTests` already proves this).

---

## Stage 4 — Supporting protocols (≈ half a day)

Each feature = one doc + one use case + one adapter + one controller. Skim, you'll return as needed.

| Feature | Doc | Key code |
|---|---|---|
| Introspection (RFC 7662) | [features/06](documentation/features/06-token-introspection.md) | `usecase/introspection/`, [OAuth2TokenIntrospectionController](src/main/java/io/github/edmaputra/enhauthserv/adapter/in/http/OAuth2TokenIntrospectionController.java) |
| Revocation (RFC 7009) | [features/07](documentation/features/07-token-revocation.md) | `usecase/revocation/`, [OAuth2TokenRevocationController](src/main/java/io/github/edmaputra/enhauthserv/adapter/in/http/OAuth2TokenRevocationController.java) |
| User consent | [features/08](documentation/features/08-consent.md) | `usecase/consent/`, [OAuth2AuthorizationConsentController](src/main/java/io/github/edmaputra/enhauthserv/adapter/in/http/OAuth2AuthorizationConsentController.java) |
| Client mgmt & bootstrap | [features/09](documentation/features/09-client-management.md) | `usecase/registration/` |
| Session & logout | [features/10](documentation/features/10-session-logout.md) | [LoggedOutController](src/main/java/io/github/edmaputra/enhauthserv/adapter/in/http/LoggedOutController.java) |

---

## Stage 5 — Persistence, config & data (≈ half a day)

1. [documentation/features/11-persistence-schema.md](documentation/features/11-persistence-schema.md) and [docs/database_schema.md](docs/database_schema.md).
2. Flyway migrations, read in version order: [src/main/resources/db/migration/](src/main/resources/db/migration/) (V0_0_1_001 → 007). The last one adds tenant discriminator columns — tie it back to Stage 3a.
3. Full config reference: [documentation/features/12-configuration.md](documentation/features/12-configuration.md) and [src/main/resources/application.properties](src/main/resources/application.properties).

---

## Stage 6 — Working in the codebase

**Build & test:**
```bash
./mvnw clean package            # Build
./mvnw test                     # All tests
./mvnw -Dtest=ClassName test    # Single test class
```

**Testing patterns to internalize** (full notes in [CLAUDE.md](CLAUDE.md)):
- Integration tests extend `AuthServerIntegrationTests`; unit tests use Mockito + AssertJ.
- H2 state leaks between Spring contexts — use a unique `spring.datasource.url` in `@TestPropertySource` when overriding props.
- Unauthenticated `/oauth2/token` falls through to the login page, not `invalid_client` — pass bad creds to force the error.
- Custom introspection/revocation endpoints must be registered explicitly in `SecurityConfig`.

**Before you push:** run `./mvnw test` — `ArchitectureBoundariesTests` will catch layer violations,
so a green build also means you respected the hexagonal boundaries.

---

## Where to go next — the roadmap

Once the current system makes sense, see what's planned and pick up work:
[documentation/roadmap/README.md](documentation/roadmap/README.md) covers authentication,
user lifecycle, federation, authorization, protocols, security hardening, operability, compliance,
and developer experience.

---

## Quick reference

| Thing | Where |
|---|---|
| Build / run / test | [CLAUDE.md](CLAUDE.md) |
| Endpoint catalog | [docs/api_endpoints.md](docs/api_endpoints.md) |
| Feature deep-dives | [documentation/features/](documentation/features/) |
| Architecture rules (enforced) | [ArchitectureBoundariesTests](src/test/java/io/github/edmaputra/enhauthserv/ArchitectureBoundariesTests.java) |
| Bean wiring | [UseCaseWiringConfig](src/main/java/io/github/edmaputra/enhauthserv/config/UseCaseWiringConfig.java), [SecurityConfig](src/main/java/io/github/edmaputra/enhauthserv/config/SecurityConfig.java) |
| Test credentials | [CLAUDE.md](CLAUDE.md) → Built-in Test Credentials |
| Roadmap | [documentation/roadmap/](documentation/roadmap/) |
