# Hexagonal Architecture Template - AI Agent Instructions

Act as a world-class peer developer and architect. Assume expert-level knowledge. Be clear, direct, and rigorous. Always reason about trade-offs and maintain architectural alignment.

## Architecture Overview

**Clean Architecture/Hexagonal Architecture** with **multi-module Maven**:

- **`domain/`** - Pure business logic, framework-free core
  - `api/` - Inbound ports (use cases, DTOs, domain errors)
  - `spi/` - Outbound ports (repository interfaces, external clients)
  - `model/` - Domain entities and value objects
  - `usecase/` - Business logic implementations
- **`infrastructure/`** - Spring Boot application with adapters
  - `config/` - Spring configurations
  - `web/` - REST controllers (inbound adapters)
  - `persistence/` - JPA repositories (outbound adapters)
  - `client/` - External service clients

## Critical Constraints

**Domain module MUST remain framework-free:**

- NO Spring/JPA/HTTP/logging imports
- NO framework annotations (`@Service`, `@Repository`, etc.)
- Enforced by ArchUnit tests in `infrastructure/src/test/java/.../architecture/ArchitectureTest.java`

## Build & Quality

```bash
# Full build with tests
mvn clean verify

# Architecture validation
mvn test -Dtest=ArchitectureTest

# Mutation testing (≥70% required)
mvn -pl domain org.pitest:pitest-maven:mutationCoverage
```

## Coding Conventions

**Immutability:** `record` for VOs/DTOs; entities expose behavior, not setters
**Nullability:** Never return `null`; `Optional` only at boundaries
**Time/Money:** `Instant`/`OffsetDateTime` in UTC; custom `Money` VO (no `double`)
**Equality:** VOs by value; entities by identity
**Naming:** Domain vocabulary; verb-based use cases (`CreateInvoice`, `PostPayment`)

## Error Handling

Map domain exceptions to Problem Details JSON:

- 400: validation errors
- 404: not found
- 409: conflict
- 422: business rule violations
- 500: unknown errors

## Testing Strategy

**Domain:** Unit + property-based tests; PIT mutation ≥70% on model/usecase
**Use Cases:** Test with stubbed SPI ports; verify orchestration & idempotency
**Infrastructure:**

- Web: `@WebMvcTest` + contract tests vs OpenAPI
- Data: `@DataJpaTest` + Testcontainers
- Clients: WireMock for HTTP; test retry/timeout

**Coverage:** ≥90% domain, moderate infrastructure

## Security & Observability

**Security:** Validate at boundaries (Bean Validation in infra); AuthN/Z in infra only
**Logging:** Domain silent; infra uses slf4j with correlation IDs (MDC)
**Metrics:** Expose timers per use case; propagate tracing headers

Start with domain interfaces and models, then implement infrastructure adapters.
