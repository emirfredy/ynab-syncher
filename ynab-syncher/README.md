# YNAB-Syncher

A hexagonal architecture application for synchronizing bank transactions with YNAB (You Need A Budget).

## Project Structure

```
ynab-syncher/
├── domain/           # Framework-free business logic
├── infrastructure/   # Spring Boot application & adapters
├── scripts/          # Utility scripts for development
└── pom.xml          # Multi-module Maven configuration
```

## Development Scripts

The `scripts/` directory contains utility scripts for common development tasks:

### Branch Management

- **`merge-to-master.sh`** - Safely merge feature branches to master with testing
- **`dry-run-merge.sh`** - Validate merge prerequisites without performing operations

```bash
# Validate current branch can be merged
./scripts/dry-run-merge.sh

# Merge current branch to master
./scripts/merge-to-master.sh

# Merge specific branch to master
./scripts/merge-to-master.sh feature/new-feature
```

See [`scripts/README.md`](scripts/README.md) for detailed documentation.

## Building & Testing

### Quick Start

```bash
# Quick test execution (recommended for development)
./scripts/run-tests.sh --quick

# Super-fast parallel execution (Phase 2)
./scripts/run-tests.sh --quick --parallel

# Full comprehensive test suite
./scripts/run-tests.sh

# Parallel execution for faster feedback (60% faster)
./scripts/run-tests.sh --parallel

# Architecture tests only
./scripts/run-tests.sh --only architecture

# Domain module tests with fail-fast
./scripts/run-tests.sh --module domain --fail-fast

# Multiple tests in parallel with verbose output
./scripts/run-tests.sh --only architecture unit wiremock --parallel --verbose
```

### Manual Maven Commands

```bash
# Build entire project
mvn clean compile

# Run all tests
mvn test

# Run tests with coverage
mvn verify

# Build specific module
mvn -pl domain clean test
mvn -pl infrastructure clean test
```

### Enhanced Test Script Features

The `run-tests.sh` script provides production-grade test automation with:

**Core Features:**

- **`--quick`** - Essential tests only (1-2 minutes)
- **`--fail-fast`** - Stop on first failure with debugging suggestions
- **`--only <types>`** - Run specific test categories (`architecture`, `unit`, `integration`, `mutation`, `wiremock`, `build`)
- **`--module <name>`** - Target specific module (`domain` or `infrastructure`)
- **`--verbose`** - Detailed execution information
- **`--help`** - Comprehensive usage guide

**Parallel Execution (Phase 2):**

- **`--parallel`** - Run independent tests concurrently for faster execution
- **`--jobs <n>`** - Control number of parallel jobs (default: auto-detect CPU cores)
- **Smart Dependency Management** - Independent tests run in parallel, dependent tests run sequentially
- **Performance Boost** - Up to 60% faster execution in parallel mode

**Interactive Development Mode (Phase 3):**

- **`--interactive`** - TUI-based test selection menu with real-time status display
- **Last Test Results** - Shows status of previously executed tests with pass/fail indicators
- **Quick Selection** - Easy access to common test combinations (quick feedback, architecture validation, etc.)
- **Future-Ready** - Prepared for watch mode, cache management, and quality dashboard features

**Usage Examples:**

```bash
# Development workflow - quick feedback with parallel execution
./scripts/run-tests.sh --quick --parallel --fail-fast

# Interactive development mode (Phase 3) - TUI test selection
./scripts/run-tests.sh --interactive

# Architecture validation before structural changes
./scripts/run-tests.sh --only architecture --fail-fast

# Full test suite with parallel execution for CI/CD
./scripts/run-tests.sh --parallel --fail-fast

# Domain-specific testing with custom parallel jobs
./scripts/run-tests.sh --module domain --parallel --jobs 4

# Unit and integration tests only, parallel execution
./scripts/run-tests.sh --only unit integration --parallel

# Comprehensive validation with mutation testing (sequential)
./scripts/run-tests.sh --verbose
```

**Performance Benefits:**

- **Sequential Mode**: ~4-5 minutes for full test suite
- **Parallel Mode**: ~2-3 minutes for full test suite (up to 60% faster)
- **Quick + Parallel**: ~1-2 minutes for essential tests only

See [`scripts/README.md`](scripts/README.md) for complete documentation.

## Architecture

This project follows hexagonal architecture principles:

- **Domain Layer**: Framework-free business logic with ports and use cases
- **Infrastructure Layer**: Spring Boot adapters for external systems (YNAB API, REST controllers)
- **Clean Separation**: Domain has no dependencies on frameworks
- **Testability**: Comprehensive unit and integration testing

The architecture is enforced by ArchUnit tests in the infrastructure module.

### Category Mapping Persistence

The category-learning feedback loop now persists mappings in PostgreSQL via Spring Data JDBC and Flyway migrations.  
Provide standard datasource properties (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) when running the Spring Boot app.  
Database changes are tracked in versioned migrations under `infrastructure/src/main/resources/db/migration`. Integration tests automatically run these migrations against the Testcontainers PostgreSQL instance.

## Contributing

1. Create a feature branch from `master`
2. Implement changes following hexagonal architecture principles
3. Ensure all tests pass: `mvn test`
4. Use dry-run script to validate: `./scripts/dry-run-merge.sh`
5. Merge using: `./scripts/merge-to-master.sh`

## License

[Add your license here]
