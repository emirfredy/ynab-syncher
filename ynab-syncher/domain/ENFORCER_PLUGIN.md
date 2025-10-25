# Maven Enforcer Plugin - Domain Architecture Protection

## Overview

The Maven Enforcer Plugin has been added to the domain module to enforce clean architecture principles by preventing external framework dependencies.

## What it enforces

### ❌ Banned Dependencies (Compile/Runtime scope)

- **Spring Framework**: `org.springframework:*`, `org.springframework.boot:*`, etc.
- **JPA/Hibernate**: `org.hibernate:*`, `jakarta.persistence:*`, `javax.persistence:*`
- **Web/HTTP**: `jakarta.servlet:*`, `org.apache.httpcomponents:*`, `com.squareup.okhttp3:*`
- **JSON Libraries**: `com.fasterxml.jackson.core:*`, `com.google.gson:*`
- **Logging Frameworks**: `org.slf4j:*`, `ch.qos.logback:*`, `org.apache.logging.log4j:*`
- **Validation Frameworks**: `jakarta.validation:*`, `org.hibernate.validator:*`

### ✅ Allowed Dependencies

- **JDK classes only** (java._, javax._ from JDK)
- **Test dependencies** (test scope): JUnit, AssertJ, Mockito

## Usage

### Validation during build

The enforcer runs automatically during the `validate` phase:

```bash
./mvnw -pl domain validate
./mvnw -pl domain compile
./mvnw -pl domain test
```

### Manual validation

```bash
./mvnw -pl domain enforcer:enforce
```

## Error Example

If someone tries to add a Spring dependency:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>6.0.0</version>
</dependency>
```

The build will fail with:

```
Domain module must remain framework-free!
Found prohibited dependency. The domain should only contain:
- Pure Java (java.* packages)
- Business logic and domain models
- Test dependencies (test scope only)

Please move framework dependencies to the infrastructure module.
```

## Architecture Benefits

1. **Enforces Clean Architecture**: Domain remains pure business logic
2. **Prevents Framework Coupling**: No accidental framework dependencies
3. **Clear Separation**: Infrastructure concerns stay in infrastructure module
4. **Build-time Safety**: Catches violations early in the development process
5. **Documentation**: Makes architectural constraints explicit and automated
