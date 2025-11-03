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

# Full comprehensive test suite
./scripts/run-tests.sh

# Architecture tests only
./scripts/run-tests.sh --only architecture

# Domain module tests with fail-fast
./scripts/run-tests.sh --module domain --fail-fast
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

- **`--quick`** - Essential tests only (1-2 minutes)
- **`--fail-fast`** - Stop on first failure with debugging suggestions
- **`--only <types>`** - Run specific test categories (`architecture`, `unit`, `integration`, `mutation`, `wiremock`, `build`)
- **`--module <name>`** - Target specific module (`domain` or `infrastructure`)
- **`--verbose`** - Detailed execution information
- **`--help`** - Comprehensive usage guide

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
