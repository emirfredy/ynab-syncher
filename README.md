# YNAB Syncher

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Mutation Testing](https://img.shields.io/badge/Mutation%20Coverage-94%25-brightgreen.svg)](https://pitest.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal-blue.svg)](https://alistair.cockburn.us/hexagonal-architecture/)
[![Build Status](https://github.com/emirfredy/ynab-syncher/workflows/CI/badge.svg)](https://github.com/emirfredy/ynab-syncher/actions)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A robust transaction reconciliation service that synchronizes bank transactions with YNAB (You Need A Budget), built with **Hexagonal Architecture** and industry-leading testing practices.

## 🎯 **Key Features**

- 🏛️ **Clean Hexagonal Architecture** with framework-free domain
- 💰 **YNAB Integration** with milliunits money format support
- 🔄 **Flexible Reconciliation Strategies** (STRICT/RANGE matching)
- 🧪 **94% Mutation Testing Coverage** (exceeds industry standards)
- 🛡️ **Architecture Enforcement** with Maven Enforcer + ArchUnit
- ⚡ **Spring Boot 3.5.7** with Java 21 modern features
- 📊 **Comprehensive Testing** with 175 tests

## 🏗️ **Architecture Overview**

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   REST API      │  │   Persistence   │  │  External    │ │
│  │   Controllers   │  │   Adapters      │  │  Clients     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer (Pure)                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Use Cases     │  │   Entities      │  │    Ports     │ │
│  │                 │  │ & Value Objects │  │ (API & SPI)  │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### **Domain Layer (Framework-Free)**

- **Value Objects**: `Money`, `AccountId`, `TransactionId`
- **Entities**: `Transaction` with business logic
- **Use Cases**: `ReconcileTransactionsUseCase`
- **Strategies**: `StrictTransactionMatcher`, `RangeTransactionMatcher`
- **Ports**: Clean API (inbound) and SPI (outbound) interfaces

### **Infrastructure Layer**

- **Web Controllers**: REST API endpoints
- **Persistence**: JPA repository adapters
- **External Clients**: YNAB API and bank integration clients
- **Configuration**: Spring Boot application setup

## 🚀 **Quick Start**

### **Prerequisites**

- Java 21+
- Maven 3.9+ (or use included wrapper)
- Git

### **Clone and Build**

```bash
git clone https://github.com/emirfredy/ynab-syncher.git
cd ynab-syncher

# Run all tests with mutation coverage
./mvnw clean verify

# Run architecture validation
./mvnw -pl infrastructure test -Dtest=ArchitectureTest

# Start the application
./mvnw -pl infrastructure spring-boot:run
```

### **Run Mutation Testing**

```bash
# Generate mutation testing report
./mvnw -pl domain org.pitest:pitest-maven:mutationCoverage

# View results
open domain/target/pit-reports/*/index.html
```

## 📊 **Quality Metrics**

| Metric                 | Value         | Threshold |
| ---------------------- | ------------- | --------- |
| **Line Coverage**      | 96% (158/165) | > 90%     |
| **Mutation Coverage**  | 94% (67/71)   | > 70%     |
| **Test Strength**      | 100%          | > 95%     |
| **Total Tests**        | 175           | -         |
| **Architecture Tests** | 12            | -         |
| **Build Time**         | ~8 seconds    | < 30s     |

## 🔧 **Reconciliation Strategies**

### **STRICT Strategy**

- Exact date matching
- Precise amount matching
- Account verification
- Best for: Automated imports with reliable data

### **RANGE Strategy**

- 3-day window matching (configurable)
- Amount tolerance handling
- Fuzzy description matching
- Best for: Manual entry reconciliation

```java
// Example usage
var request = ReconciliationRequest.builder()
    .accountId(AccountId.of("account-123"))
    .bankTransactions(bankTransactions)
    .ynabTransactions(ynabTransactions)
    .strategy(ReconciliationStrategy.RANGE)
    .build();

var result = reconcileTransactionsUseCase.execute(request);
```

## 🧪 **Testing Strategy**

### **Domain Testing (163 tests)**

- **Unit Tests**: Pure domain logic validation
- **Property-Based Tests**: Value object invariants
- **Strategy Tests**: Transaction matching algorithms
- **Use Case Tests**: Business workflow validation

### **Architecture Testing (12 tests)**

- **Domain Independence**: Framework-free validation
- **Ports & Adapters**: Interface compliance
- **Package Organization**: Structure validation
- **Layered Architecture**: Dependency rules

### **Mutation Testing**

```bash
# Configured with PIT for maximum quality
- Threshold: 70% (we achieve 94%)
- Mutators: ALL (comprehensive)
- Target: domain module only
- Integration: Maven + JUnit 5
```

## 🛡️ **Architecture Protection**

### **Build-Time Protection (Maven Enforcer)**

```xml
<!-- Prevents framework dependencies in domain -->
<bannedDependencies>
  <excludes>
    <exclude>org.springframework:*</exclude>
    <exclude>jakarta.persistence:*</exclude>
    <exclude>org.hibernate:*</exclude>
    <!-- ... more exclusions -->
  </excludes>
</bannedDependencies>
```

### **Test-Time Validation (ArchUnit)**

```java
// Validates hexagonal architecture principles
@Test
void domainShouldNotDependOnInfrastructureFrameworks() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..")
        .check(allClasses);
}
```

## 📁 **Project Structure**

```
ynab-syncher/
├── domain/                          # 🏛️ Pure business logic
│   ├── src/main/java/
│   │   ├── api/usecase/            # Inbound ports
│   │   ├── model/                  # Entities & Value Objects
│   │   │   ├── matcher/           # Transaction matching strategies
│   │   │   └── reconciliation/    # Reconciliation workflow
│   │   ├── spi/repository/         # Outbound ports
│   │   └── usecase/               # Business logic implementation
│   └── src/test/java/             # 163 comprehensive tests
├── infrastructure/                  # 🔌 Framework adapters
│   ├── src/main/java/
│   │   ├── config/                # Spring configuration
│   │   ├── web/                   # REST controllers
│   │   ├── persistence/           # JPA repositories
│   │   └── client/                # External API clients
│   └── src/test/java/
│       └── architecture/          # ArchUnit tests
├── .github/
│   ├── workflows/                 # CI/CD pipelines
│   └── ISSUE_TEMPLATE/           # GitHub templates
└── docs/                         # 📚 Documentation
```

## 🔗 **Integration Points**

### **YNAB API**

- OAuth 2.0 authentication
- Milliunits money format (1000 = $1.00)
- Transaction categorization
- Account synchronization

### **Bank APIs**

- OFX/QFX import support
- CSV transaction parsing
- Real-time transaction feeds
- Multi-bank support

## 🚧 **Development**

### **Adding New Features**

1. **Domain First**: Implement in pure domain layer
2. **Test Coverage**: Ensure >90% line coverage + mutation testing
3. **Architecture**: Validate with ArchUnit tests
4. **Integration**: Add infrastructure adapters

### **Code Quality Gates**

- ✅ All tests pass (175 tests)
- ✅ Mutation coverage >70% (currently 94%)
- ✅ Architecture tests pass (12 tests)
- ✅ Maven Enforcer validation
- ✅ No framework dependencies in domain

## 📖 **Documentation**

- [Architecture Decision Records](docs/adr/)
- [API Documentation](docs/api/)
- [Deployment Guide](docs/deployment/)
- [Contributing Guidelines](CONTRIBUTING.md)

## 🤝 **Contributing**

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Ensure all quality gates pass (`./mvnw clean verify`)
4. Commit with conventional commits (`git commit -m 'feat: add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 **Acknowledgments**

- [YNAB API](https://api.youneedabudget.com/) for excellent financial data access
- [ArchUnit](https://www.archunit.org/) for architecture testing
- [PIT](http://pitest.org/) for mutation testing excellence
- [Spring Boot](https://spring.io/projects/spring-boot) for robust framework foundation

---

**Built with ❤️ using Hexagonal Architecture and Test-Driven Development**
