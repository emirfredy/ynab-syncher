# Hexagonal Architecture Template - AI Agent Instructions

Act as a world-class peer developer and architect. Assume expert-level knowledge. Be clear, direct, and rigorous. Always reason about trade-offs and maintain architectural alignment.

## Core Architectural Principles

**Clean Architecture/Hexagonal Architecture** with **Netflix/Uber Microservices Pattern**

### Critical Constraints (Non-Negotiable)

**Domain module MUST remain framework-free:**

- NO Spring/JPA/HTTP/logging imports
- NO framework annotations (`@Service`, `@Repository`, etc.)
- Enforced by ArchUnit tests in `infrastructure/src/test/java/.../architecture/ArchitectureTest.java`

**ArchUnit Enforcement Rules:**

- **Domain Independence**: Domain module cannot import ANY infrastructure dependencies
- **Dependency Direction**: Infrastructure → Application Service → Domain (never reversed)
- **Layer Boundaries**: Controllers cannot directly access domain use cases
- **Application Services**: Allowed to access domain API for orchestration
- **Mappers**: Permitted domain model access for DTO conversion

### Netflix/Uber Microservices Pattern

**Layer Responsibilities:**

- **REST Controllers**: HTTP protocol concerns ONLY

  - Status codes, headers, correlation IDs, validation
  - Single dependency: Application Service (NOT domain use cases)

- **Application Services**: Orchestration layer following Netflix/Uber pattern

  - DTO mapping between Web DTOs ↔ Domain DTOs
  - Transaction boundaries and cross-cutting concerns
  - Multi-step workflow coordination using domain use cases
  - Example: `YnabSyncApplicationService` orchestrates import → reconcile → sync

- **Web DTOs**: Infrastructure layer API contracts
  - Use "Web" suffix: `*WebRequest`, `*WebResponse` (avoid naming collisions)
  - Optimized for HTTP transport (strings, JSON-friendly types)
  - Bean Validation annotations for request validation

**Anti-Patterns to Avoid:**

- ❌ Controllers directly injecting domain use cases
- ❌ Anemic service layers (pass-through with no orchestration value)
- ❌ "Internal" prefix for DTOs
- ❌ 1:1 mapping between service methods and use cases without adding value

### Implementation Approach

**Sequence:** Application Services → Web DTOs → DTO Mappers → REST Controllers → Tests

**Key Benefits:**

- Cross-cutting concerns at application service level
- Controllers focus on HTTP protocol only
- Future-proof for caching, security, complex workflows
- Industry standard pattern for microservices

### Testing Strategy & Verification

**Domain Layer:**

- Unit + property-based tests with stubbed SPI ports
- Mutation testing ≥70% kill rate (PIT: `mvn -pl domain org.pitest:pitest-maven:mutationCoverage`)
- Coverage ≥90%

**Infrastructure Layer:**

- **Controllers** (`@WebMvcTest`): Mock application service only; test HTTP concerns
- **Application Services**: Mock domain use cases; test orchestration and DTO mapping
- **Persistence**: `@DataJpaTest` + Testcontainers
- **Clients**: WireMock for HTTP integration testing

### Quality Gates & Task Completion

**Before considering any task complete:**

1. **Architecture Compliance** (`mvn test -Dtest=ArchitectureTest`)

   - Domain independence maintained
   - Proper dependency direction enforcement
   - Layer boundaries enforced

2. **Testing Standards**

   - Domain: ≥70% mutation score, ≥90% coverage
   - Infrastructure: HTTP concerns (controllers), orchestration (services)

3. **Build Success** (`mvn clean verify`)

   - All tests passing
   - No compilation errors

4. **Production Readiness**
   - Correlation ID propagation
   - Bean validation at boundaries
   - Global exception handler

**CRITICAL: Task Completion Validation**

- **Quick Validation**: Run `./scripts/run-tests.sh --quick --fail-fast` for rapid feedback
- **Architecture Validation**: Run `./scripts/run-tests.sh --only architecture --fail-fast` before structural changes
- **Module-Specific**: Run `./scripts/run-tests.sh --module domain --fail-fast` for domain changes
- **Full Validation**: Run `./scripts/run-tests.sh --fail-fast` for comprehensive validation
- **All must pass** before considering any implementation task complete

**Enhanced Testing Options:**

- Use `--fail-fast` for immediate error feedback with debugging suggestions
- Use `--only <types>` to focus on specific test categories (`architecture`, `unit`, `integration`, `mutation`)
- Use `--module <name>` to target `domain` or `infrastructure` modules
- Use `--verbose` for detailed execution information

## Module Structure & Technology Specifics

**Multi-module Maven Project:**

- **`domain/`** - Pure business logic, framework-free core
  - `api/` - Inbound ports (use cases, DTOs, domain errors)
  - `spi/` - Outbound ports (repository interfaces, external clients)
  - `model/` - Domain entities and value objects
  - `usecase/` - Business logic implementations
- **`infrastructure/`** - Spring Boot application with adapters
  - `config/` - Spring configurations
  - `web/` - REST controllers (inbound adapters)
  - `service/` - Application services (Netflix/Uber pattern)
  - `persistence/` - JPA repositories (outbound adapters)
  - `client/` - External service clients

### Coding Conventions

**Immutability:** `record` for VOs/DTOs; entities expose behavior, not setters
**Nullability:** Never return `null`; `Optional` only at boundaries
**Time/Money:** `Instant`/`OffsetDateTime` in UTC; custom `Money` VO (no `double`)
**Equality:** VOs by value; entities by identity
**Naming:** Domain vocabulary; verb-based use cases (`CreateInvoice`, `PostPayment`)

### Production Requirements

**Observability:**

- **Correlation IDs**: Generated per request, propagated via MDC
- **Logging**: Domain silent; infrastructure uses slf4j with structured JSON
- **Metrics**: `@Timed` on controllers and application services
- **Headers**: `X-Correlation-ID` in responses

**Security & Validation:**

- Bean Validation at controller boundaries only
- AuthN/Z in infrastructure layer only
- Map domain exceptions to Problem Details JSON (400/404/409/422/500)

**Error Handling:**

- Global Exception Handler for consistent responses
- Rate limiting awareness for external APIs

### Build & Quality Commands

```bash
# Full build with tests
mvn clean verify

# Architecture validation
mvn test -Dtest=ArchitectureTest

# Mutation testing (≥70% required)
mvn -pl domain org.pitest:pitest-maven:mutationCoverage

# Fast test execution (our custom script)
./scripts/run-tests.sh
```

**run-tests.sh Enhanced Script Usage:**

```bash
# Interactive development mode (Phase 3) - TUI test selection
./scripts/run-tests.sh --interactive

# Quick development feedback (parallel by default, 60% faster)
./scripts/run-tests.sh --quick --fail-fast

# Architecture validation before structural changes
./scripts/run-tests.sh --only architecture --fail-fast

# Domain-specific testing during feature development
./scripts/run-tests.sh --module domain --fail-fast

# Unit and integration tests (parallel by default)
./scripts/run-tests.sh --only unit integration --verbose

# Full comprehensive test suite (parallel by default)
./scripts/run-tests.sh --fail-fast

# Force sequential execution when needed
./scripts/run-tests.sh --sequential --verbose
```

**Enhanced Script Benefits:**

- **Parallel by Default**: Optimized performance with 60% faster execution out of the box
- **Interactive Development Mode**: `--interactive` provides TUI-based test selection with real-time status display
- **Fail-Fast Mode**: `--fail-fast` provides immediate error feedback with debugging suggestions
- **Intelligent Filtering**: `--only <types>` and `--module <name>` for targeted testing
- **Quick Feedback**: `--quick` flag skips slow mutation testing for rapid development cycles
- **Smart Dependency Management**: Independent tests run in parallel, dependent tests run sequentially
- **Comprehensive Validation**: Full execution includes mutation testing and coverage analysis
- **Detailed Reporting**: Comprehensive test results summary with timing and metrics
- **CI Integration**: Same script used in development and CI pipeline

### Refactoring Anti-Patterns Prevention

**Test-Driven Domain Design**

- Write domain tests FIRST based on requirements (Red-Green-Refactor)
- Domain behaviors drive implementation, not architectural patterns
- Never add deprecated methods in greenfield projects

**Continuous Integration Validation**

- ArchUnit tests enforce domain independence on every commit
- Mutation testing validates test effectiveness in CI pipeline
- Breaking change detection prevents API degradation
- No premature versioning (use "Unreleased" until actual deployment)

**Focused Scope Management**

- Single responsibility per feature branch with clear success criteria
- Time-boxed spikes for exploration (max 2 hours before deciding)
- MVP mindset: simplest working solution first, evolve complexity
- Document scope boundaries: what's IN vs OUT of current work

Follow the Netflix/Uber pattern for production REST APIs while maintaining hexagonal architecture principles.
