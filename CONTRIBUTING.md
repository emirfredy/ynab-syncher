# Contributing to YNAB Syncher

Thank you for your interest in contributing to YNAB Syncher! This document provides guidelines and information for contributors.

## 🎯 **Code of Conduct**

This project follows a code of conduct to ensure a welcoming environment for everyone. Please be respectful and constructive in all interactions.

## 🏗️ **Architecture Principles**

Before contributing, please understand our core architectural principles:

### **Hexagonal Architecture**

- **Domain Layer**: Pure business logic, no framework dependencies
- **Infrastructure Layer**: Adapters for external systems
- **Ports**: Clean interfaces between layers

### **Quality Standards**

- **Test Coverage**: Minimum 90% line coverage
- **Mutation Coverage**: Minimum 70% (currently 94%)
- **Architecture Tests**: All ArchUnit tests must pass
- **Code Style**: Consistent formatting and naming conventions

## 🚀 **Getting Started**

### **Prerequisites**

- Java 21+
- Maven 3.9+
- Git
- IDE with Java support (IntelliJ IDEA recommended)

### **Development Setup**

```bash
# Clone the repository
git clone https://github.com/emirfredy/ynab-syncher.git
cd ynab-syncher

# Run full test suite
./mvnw clean verify

# Run mutation testing
./mvnw -pl domain org.pitest:pitest-maven:mutationCoverage

# Start application
./mvnw -pl infrastructure spring-boot:run
```

## 📋 **Development Workflow**

### **1. Create Feature Branch**

```bash
git checkout -b feature/your-feature-name
```

### **2. Follow TDD Approach**

1. **Red**: Write failing test first
2. **Green**: Implement minimal code to pass
3. **Refactor**: Improve code while keeping tests green

### **3. Maintain Quality Gates**

- All tests must pass (175+ tests)
- Mutation coverage must stay ≥70%
- Architecture tests must pass
- No framework dependencies in domain

### **4. Commit Guidelines**

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Feature
git commit -m "feat: add transaction reconciliation strategy"

# Bug fix
git commit -m "fix: handle null transaction dates"

# Documentation
git commit -m "docs: update API documentation"

# Test
git commit -m "test: add mutation tests for Money value object"

# Refactor
git commit -m "refactor: extract transaction matcher factory"
```

## 🧪 **Testing Guidelines**

### **Domain Layer Testing**

```java
// Example: Value Object Test
@Test
@DisplayName("Should create Money with positive milliunits")
void shouldCreateMoneyWithPositiveMilliunits() {
    // Given
    long milliunits = 1000; // $1.00

    // When
    Money money = Money.of(milliunits);

    // Then
    assertThat(money.milliunits()).isEqualTo(1000);
    assertThat(money.dollars()).isEqualTo(BigDecimal.ONE);
}
```

### **Property-Based Testing**

```java
@Test
@DisplayName("Money creation should be consistent")
void moneyCreationShouldBeConsistent() {
    // Property: Money created from dollars should equal milliunits
    for (int i = 0; i < 100; i++) {
        BigDecimal dollars = BigDecimal.valueOf(random.nextDouble() * 1000);
        Money fromDollars = Money.fromDollars(dollars);
        Money fromMilliunits = Money.of(dollars.multiply(new BigDecimal("1000")).longValue());

        assertThat(fromDollars).isEqualTo(fromMilliunits);
    }
}
```

### **Architecture Testing**

```java
@Test
@DisplayName("Domain should not depend on infrastructure frameworks")
void domainShouldNotDependOnInfrastructureFrameworks() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "javax.persistence..")
        .check(allClasses);
}
```

## 📁 **Project Structure**

```
ynab-syncher/
├── domain/                          # 🏛️ Pure business logic
│   ├── src/main/java/
│   │   ├── api/                    # Inbound ports (use cases)
│   │   ├── model/                  # Entities & Value Objects
│   │   │   ├── matcher/           # Transaction matching strategies
│   │   │   └── reconciliation/    # Reconciliation workflow
│   │   ├── spi/                   # Outbound ports (repositories)
│   │   └── usecase/               # Business logic implementation
│   └── src/test/java/             # Domain tests (163 tests)
├── infrastructure/                  # 🔌 Framework adapters
│   ├── src/main/java/
│   │   ├── config/                # Spring configuration
│   │   ├── web/                   # REST controllers
│   │   ├── persistence/           # JPA repositories
│   │   └── client/                # External API clients
│   └── src/test/java/
│       └── architecture/          # ArchUnit tests (12 tests)
└── docs/                          # 📚 Documentation
```

## 🔄 **Adding New Features**

### **Domain-First Approach**

1. **Start with Domain**: Implement business logic first
2. **Add Tests**: Comprehensive test coverage with mutation testing
3. **Create Ports**: Define clean interfaces
4. **Implement Adapters**: Add infrastructure implementations

### **Example: Adding New Reconciliation Strategy**

```java
// 1. Domain: Add new strategy enum
public enum ReconciliationStrategy {
    STRICT, RANGE, FUZZY  // <- New strategy
}

// 2. Domain: Implement strategy
public class FuzzyTransactionMatcher implements TransactionMatcher {
    // Implementation with business logic
}

// 3. Test: Comprehensive testing
@Test
@DisplayName("Fuzzy matcher should handle description variations")
void fuzzyMatcherShouldHandleDescriptionVariations() {
    // Test implementation
}

// 4. Infrastructure: Wire in factory
@Bean
public TransactionMatcherFactory transactionMatcherFactory() {
    // Add fuzzy matcher to factory
}
```

## 📊 **Quality Gates**

All contributions must pass these quality gates:

### **Automated Checks**

- ✅ All 175+ tests pass
- ✅ Mutation coverage ≥70%
- ✅ Architecture tests pass
- ✅ Maven Enforcer validation
- ✅ No framework dependencies in domain

### **Code Review Checklist**

- [ ] Follows hexagonal architecture principles
- [ ] Comprehensive test coverage
- [ ] Clear, self-documenting code
- [ ] Appropriate use of value objects and entities
- [ ] Error handling and validation
- [ ] Documentation updates

## 🐛 **Bug Reports**

When reporting bugs, please include:

- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (Java version, OS)
- Log output (if applicable)

Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.yml) for consistency.

## ✨ **Feature Requests**

When suggesting features, please include:

- Problem or use case being addressed
- Proposed solution
- Alternative approaches considered
- Component impact analysis

Use our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.yml) for clarity.

## 📖 **Documentation**

### **Code Documentation**

- JavaDoc for all public APIs
- Clear method and class names
- Meaningful variable names
- Comments for complex business logic

### **Architecture Documentation**

- Update ADRs for significant decisions
- Maintain README accuracy
- Document integration patterns
- Update API documentation

## 🔒 **Security**

### **Security Considerations**

- No sensitive data in logs
- Secure API key handling
- Input validation and sanitization
- Dependency vulnerability scanning

### **Reporting Security Issues**

Please report security vulnerabilities privately via email rather than public issues.

## 📦 **Release Process**

### **Version Numbering**

We follow [Semantic Versioning](https://semver.org/):

- **Major**: Breaking changes
- **Minor**: New features (backwards compatible)
- **Patch**: Bug fixes

### **Release Checklist**

- [ ] All tests pass
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers bumped
- [ ] Release notes prepared

## 🙏 **Recognition**

Contributors are recognized in:

- Git commit history
- Release notes
- CONTRIBUTORS.md file
- Project documentation

## 📞 **Getting Help**

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Code Reviews**: Detailed feedback on contributions

---

Thank you for contributing to YNAB Syncher! Your efforts help make financial management easier for everyone. 🚀
