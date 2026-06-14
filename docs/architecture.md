# 🏛️ Architecture & Clean Decoupling

Enhauthserv is built on **Hexagonal Architecture (Ports and Adapters)**. This architectural pattern isolates core business logic from external frameworks, databases, and delivery mechanisms (like HTTP APIs).

---

## 🏗️ Architectural Core Principles

1. **Framework-Free Core**: The business logic inside `domain` and `application` has zero dependencies on frameworks like Spring Boot, Jakarta Servlet, Hibernate, or Spring Data JPA. It is written in pure Java.
2. **Ports as Contracts**: All communication with external components (databases, security contexts, HTTP filters) is strictly defined via interfaces called **Ports**.
   - **Input Ports**: Define what operations can be performed on the core business logic (inward boundaries).
   - **Output Ports**: Define what data or services the business logic requires from the outside world (outward boundaries).
3. **Adapters as Implementations**: Framework-specific or infrastructure-specific code sits in **Adapters**.
   - **Input Adapters**: Capture external HTTP/servlet actions and convert them into commands for the input ports (e.g., Spring `@RestController` endpoints).
   - **Output Adapters**: Implement output ports to interact with H2 database, JDBC, or the Spring Security context.
4. **Wiring Layer**: The `config` package bridges the core and the adapters using Spring `@Bean` definitions, keeping adapters and use cases loosely coupled.

---

## 📁 Package Breakdown

Here is how code elements are distributed according to layers:

| Layer | Package Path | Allowed Dependencies | Purpose |
|---|---|---|---|
| **Domain** | `io.github.edmaputra.enhauthserv.domain` | None (Pure Java) | Enterprise-wide rules and schemas. |
| **Application (Ports)** | `io.github.edmaputra.enhauthserv.application.port` | Domain, Java Standard Library | Contracts for application entry (`in`) and exit (`out`). |
| **Application (Use Cases)** | `io.github.edmaputra.enhauthserv.application.usecase` | Domain, Application Ports | Coordinates tasks and executes business logic (implements input ports). |
| **Input Adapters** | `io.github.edmaputra.enhauthserv.adapter.in` | Application Ports, Spring Web, Servlet | REST controllers and filters converting web inputs to domain commands. |
| **Output Adapters** | `io.github.edmaputra.enhauthserv.adapter.out` | Application Ports, JPA Repositories | Implementations of output ports using database, cache, or framework services. |
| **Config** | `io.github.edmaputra.enhauthserv.config` | Everything | Spring Bean wiring configuration. |
| **Persistence Entities** | `io.github.edmaputra.enhauthserv.entity` / `..repository` | Jakarta Persistence, Spring Data | DB mapping definitions and standard JPA access. |

---

## ⚙️ Use Case Wiring

Use cases are instantiated and registered as Spring beans inside [UseCaseWiringConfig.java](file:///Users/bangun.saputra/Bangun/Projects/enhauthserv/src/main/java/io/github/edmaputra/enhauthserv/config/UseCaseWiringConfig.java). Controllers and adapters interact exclusively via the input ports rather than calling use-case implementations or raw repositories directly.

For example, token revocation is wired as follows:
```java
@Bean
RevokeTokenInputPort revokeTokenInputPort(
    ClientAuthenticationPort clientAuthenticationPort,
    TokenRevocationPort tokenRevocationPort,
    AuthorizationPolicyInputPort authorizationPolicyInputPort) {
  return new RevokeTokenUseCase(
      clientAuthenticationPort,
      tokenRevocationPort,
      authorizationPolicyInputPort);
}
```

---

## 🧪 Architectural Governance (ArchUnit)

Architectural boundaries are programmatically enforced by [ArchitectureBoundariesTests.java](file:///Users/bangun.saputra/Bangun/Projects/enhauthserv/src/test/java/io/github/edmaputra/enhauthserv/ArchitectureBoundariesTests.java) using **ArchUnit**. 

These tests run automatically on build (`./mvnw test`) and assert three critical invariants:

### 1. Domain Isolation
The `domain` package must have zero dependency on frameworks:
```java
noClasses()
    .that().resideInAnyPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "org.springframework..",
        "jakarta.servlet..",
        "jakarta.persistence..",
        "org.hibernate..")
```

### 2. Application Core Decoupling
The `application` layer must not import any adapters, entities, repositories, or frameworks:
```java
noClasses()
    .that().resideInAnyPackage("..application..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "..adapter..",
        "..controller..",
        "..repository..",
        "..entity..",
        "org.springframework..",
        "jakarta.servlet..",
        "jakarta.persistence..")
```

### 3. Strict Layer Ordering
The `domain` layer must not know about the application layer or adapters:
```java
noClasses()
    .that().resideInAnyPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..application..", "..adapter..")
```
