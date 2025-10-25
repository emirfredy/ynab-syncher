# ADR-002: Mutation Testing with PIT

## Status

**Accepted** - October 2025

## Context

Traditional code coverage metrics (line coverage, branch coverage) can be misleading and don't guarantee test quality. We need a way to ensure our tests actually verify business logic correctness.

## Decision

We will use PIT (Pitest) for mutation testing with a minimum threshold of 70% mutation coverage.

## Rationale

### 🎯 **What is Mutation Testing?**

Mutation testing works by:

1. Creating "mutants" - small changes to source code
2. Running tests against each mutant
3. If tests fail, the mutant is "killed" (good!)
4. If tests pass, the mutant "survives" (indicates weak tests)

### ✅ **Benefits**

- **Test Quality**: Ensures tests actually verify logic, not just execute code
- **Bug Detection**: Finds weak spots in test coverage
- **Refactoring Confidence**: Strong mutation coverage enables safe refactoring
- **Documentation**: Mutation reports show exactly what behavior is tested

### 📊 **Target Metrics**

- **Minimum Threshold**: 70% mutation coverage
- **Current Achievement**: 94% mutation coverage
- **Industry Benchmark**: 60-80% is considered excellent

## Implementation

### **Configuration (domain/pom.xml)**

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.18.0</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <targetClasses>co.personal.ynabsyncher.*</targetClasses>
        <targetTests>co.personal.ynabsyncher.*</targetTests>
        <mutationThreshold>70</mutationThreshold>
        <coverageThreshold>90</coverageThreshold>
        <timestampedReports>false</timestampedReports>
        <detectInlinedCode>true</detectInlinedCode>
        <mutators>
            <mutator>ALL</mutator>
        </mutators>
    </configuration>
</plugin>
```

### **Execution**

```bash
# Run mutation testing
./mvnw -pl domain org.pitest:pitest-maven:mutationCoverage

# View report
open domain/target/pit-reports/index.html
```

### **CI/CD Integration**

- Mutation testing runs on every build
- Build fails if mutation coverage drops below 70%
- Reports uploaded as artifacts for analysis

## Results Achieved

### **Current Metrics (as of October 2025)**

- **Mutation Coverage**: 94% (67/71 mutations killed)
- **Line Coverage**: 96% (158/165 lines)
- **Test Strength**: 100% (all covered mutations are killed)

### **Quality Breakdown by Component**

- **Value Objects**: 100% mutation coverage
- **Entities**: 95% mutation coverage
- **Use Cases**: 92% mutation coverage
- **Strategy Pattern**: 98% mutation coverage

## Consequences

### ✅ **Positive**

- **Exceptional Test Quality**: 94% mutation coverage far exceeds industry standards
- **Refactoring Safety**: High confidence in making changes
- **Bug Prevention**: Mutation testing catches logic errors missed by line coverage
- **Documentation**: Tests serve as living documentation of behavior

### ⚠️ **Trade-offs**

- **Build Time**: Mutation testing adds ~2-3 minutes to build
- **Complexity**: Understanding mutation reports requires learning
- **Maintenance**: High-quality tests require more effort to write

### 📈 **Continuous Improvement**

- Regular review of surviving mutants
- Targeted test improvements for weak areas
- Balance between coverage and test maintenance

## Alternatives Considered

### **Line Coverage Only**

- **Pros**: Fast, simple, well-understood
- **Cons**: Can be misleading, doesn't ensure test quality

### **Branch Coverage**

- **Pros**: Better than line coverage
- **Cons**: Still doesn't verify logic correctness

### **Property-Based Testing**

- **Pros**: Excellent for finding edge cases
- **Cons**: Complementary to, not replacement for, mutation testing

## Validation

This decision is validated through:

- ✅ 94% mutation coverage achieved (exceeds 70% target)
- ✅ Build pipeline integration
- ✅ Regular mutation report analysis
- ✅ Correlation with low defect rates
- ✅ Developer confidence in refactoring

## Implementation Notes

### **Focus on Domain Layer**

- Mutation testing applied only to domain module
- Infrastructure layer uses integration tests instead
- Domain business logic receives maximum scrutiny

### **Mutator Selection**

- Using "ALL" mutators for comprehensive testing
- Includes: conditionals, math, negation, returns, void calls
- Custom mutator exclusions when justified

### **Report Analysis**

- Surviving mutants reviewed in code reviews
- Equivalent mutants documented and justified
- Test improvements tracked over time

## References

- [PIT Mutation Testing](http://pitest.org/)
- [Mutation Testing: Better Code by Making Bugs](https://pedrorijo.com/blog/intro-mutation/)
- [How Mutation Testing Improves Code Quality](https://martinfowler.com/articles/mutation-testing.html)
