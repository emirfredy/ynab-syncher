# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Core Domain Model**

  - `Money` value object with milliunits precision for YNAB compatibility
  - `Transaction` entity with amount, description, and date fields
  - `Account` entity for transaction grouping
  - `TransactionMatcher` strategy pattern for flexible matching algorithms

- **Transaction Reconciliation**

  - STRICT matching strategy for exact transaction matching
  - RANGE matching strategy with configurable amount tolerance
  - `ReconciliationService` for coordinating reconciliation workflow
  - Comprehensive error handling with domain-specific exceptions

- **Architecture & Quality**

  - Hexagonal architecture with clean domain separation
  - Maven multi-module project structure
  - Maven Enforcer Plugin to prevent framework dependencies in domain
  - ArchUnit tests for architectural compliance validation
  - 94% mutation testing coverage (exceeds 70% industry standard)
  - 175+ comprehensive tests across all layers

- **Infrastructure**

  - Spring Boot 3.5.7 with Java 21 support
  - RESTful API endpoints for transaction management
  - JPA persistence layer with proper entity mapping
  - Configuration management for external integrations

- **Developer Experience**

  - Professional README with architecture diagrams and quality metrics
  - Comprehensive CONTRIBUTING.md with development workflow
  - SECURITY.md with vulnerability reporting guidelines
  - GitHub issue templates for bugs and feature requests
  - Complete documentation structure with Architecture Decision Records

- **CI/CD & Automation**
  - GitHub Actions workflow for automated testing
  - Mutation testing validation in CI pipeline
  - Architecture compliance checking
  - Security vulnerability scanning
  - Automated documentation generation and GitHub Pages setup
  - Release automation with semantic versioning

### Technical Details

- **Java**: 21 with `--release 21` compilation
- **Spring Boot**: 3.5.7
- **Testing**: JUnit 5, Mockito, AssertJ, PIT Mutation Testing 1.18.0
- **Architecture**: ArchUnit 1.2.0 for architectural validation
- **Build**: Maven 3.9+ with enforcer plugin 3.6.2
- **Quality**: 94% mutation coverage, 175+ tests, comprehensive architectural validation

### Architecture Decisions

- **ADR-001**: Hexagonal Architecture adoption for clean separation of concerns
- **ADR-002**: Mutation Testing implementation for superior code quality assurance
- **Domain-First Design**: Pure business logic without framework dependencies
- **Strategy Pattern**: Flexible transaction matching with STRICT and RANGE strategies
- **Value Objects**: Immutable financial data representation with Money milliunits

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines and [SECURITY.md](SECURITY.md) for security policies.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
