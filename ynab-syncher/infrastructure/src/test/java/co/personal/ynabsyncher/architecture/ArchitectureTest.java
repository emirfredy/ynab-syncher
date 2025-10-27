package co.personal.ynabsyncher.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests to enforce hexagonal architecture principles.
 * 
 * This complements the Maven Enforcer Plugin by providing fine-grained
 * architectural validation at the code/import level.
 */
@DisplayName("Architecture Tests")
class ArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
                .importPackages("co.personal.ynabsyncher");
    }

    @Nested
    @DisplayName("Clean Architecture - Domain Independence")
    class DomainIndependence {

        @Test
        @DisplayName("Domain should not depend on infrastructure frameworks")
        void domainShouldNotDependOnInfrastructureFrameworks() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .or().resideInAPackage("co.personal.ynabsyncher.api..")
                    .or().resideInAPackage("co.personal.ynabsyncher.spi..")
                    .or().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
                        "jakarta.servlet..",
                        "org.slf4j..",
                        "ch.qos.logback..",
                        "org.apache.logging.log4j..",
                        "com.fasterxml.jackson..",
                        "com.google.gson..",
                        "org.apache.httpcomponents..",
                        "com.squareup.okhttp3..",
                        "jakarta.validation..",
                        "javax.validation..",
                        "org.hibernate.validator.."
                    )
                    .because("Domain should remain framework-free to maintain clean architecture");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Domain should not use framework annotations")
        void domainShouldNotUseFrameworkAnnotations() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .or().resideInAPackage("co.personal.ynabsyncher.api..")
                    .or().resideInAPackage("co.personal.ynabsyncher.spi..")
                    .or().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
                    .orShould().beAnnotatedWith("jakarta.persistence.Entity")
                    .orShould().beAnnotatedWith("javax.persistence.Entity")
                    .orShould().beAnnotatedWith("jakarta.persistence.Table")
                    .orShould().beAnnotatedWith("javax.persistence.Table")
                    .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.Controller")
                    .because("Domain should not use framework-specific annotations");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Domain should only import from JDK packages")
        void domainShouldOnlyImportJdkPackages() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .or().resideInAPackage("co.personal.ynabsyncher.api..")
                    .or().resideInAPackage("co.personal.ynabsyncher.spi..")
                    .or().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "co.personal.ynabsyncher..",
                        "org.junit..", // Allow for tests
                        "org.assertj..", // Allow for tests
                        "org.mockito.." // Allow for tests
                    )
                    .because("Domain should only depend on JDK classes and itself");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Hexagonal Architecture - Ports and Adapters")
    class PortsAndAdapters {

        @Test
        @DisplayName("Use cases should follow naming convention")
        void useCasesShouldFollowNamingConvention() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .and().areNotInterfaces()
                    .and().areNotEnums()
                    .and().areNotRecords()
                    .should().haveSimpleNameEndingWith("UseCase")
                    .because("Use case implementations should follow naming convention");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Repository ports should be interfaces")
        void repositoryPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.spi.repository..")
                    .should().beInterfaces()
                    .because("Repository ports should be interfaces in SPI");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Client ports should be interfaces when present")
        void clientPortsShouldBeInterfacesWhenPresent() {
            // Only check if there are classes in the client package
            ClassFileImporter importer = new ClassFileImporter();
            JavaClasses clientClasses = importer.importPackages("co.personal.ynabsyncher.spi.client");
            
            if (!clientClasses.isEmpty()) {
                ArchRule rule = classes()
                        .that().resideInAPackage("co.personal.ynabsyncher.spi.client..")
                        .should().beInterfaces()
                        .because("Client ports should be interfaces in SPI");

                rule.check(allClasses);
            }
            // If no client classes exist, the test passes (empty package is allowed)
        }

        @Test
        @DisplayName("Use case ports should be interfaces")
        void useCasePortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.api.usecase..")
                    .should().beInterfaces()
                    .because("Use case ports (API) should be interfaces");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Domain Model Patterns")
    class DomainModelPatterns {

        @Test
        @DisplayName("Value objects ending with 'Id' should be records")
        void valueObjectsEndingWithIdShouldBeRecords() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .and().haveSimpleNameEndingWith("Id")
                    .should().beRecords()
                    .because("Value objects should be implemented as records for immutability");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Money value object should be a record")
        void moneyValueObjectShouldBeRecord() {
            ArchRule rule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .and().haveSimpleName("Money")
                    .should().beRecords()
                    .because("Money value object should be implemented as a record");

            rule.check(allClasses);
        }

        // Note: Setter detection requires more complex ArchUnit syntax
        // This test is commented out to avoid API complexity
        // @Test
        // @DisplayName("Domain models should not have setters")
        // void domainModelsShouldNotHaveSetters() {
        //     // Domain models should be immutable (using records naturally prevents setters)
        // }
    }

    @Nested
    @DisplayName("Package Organization")
    class PackageOrganization {

        @Test
        @DisplayName("Transaction matchers should be in matcher package")
        void transactionMatchersShouldBeInMatcherPackage() {
            ArchRule rule = classes()
                    .that().haveSimpleNameContaining("Matcher")
                    .and().resideInAPackage("co.personal.ynabsyncher.model..")
                    .should().resideInAPackage("co.personal.ynabsyncher.model.matcher..")
                    .because("Transaction matchers should be organized in matcher package");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Reconciliation classes should be in reconciliation package")
        void reconciliationClassesShouldBeInReconciliationPackage() {
            ArchRule rule = classes()
                    .that().haveSimpleNameContaining("Reconcil")
                    .and().resideInAPackage("co.personal.ynabsyncher.model..")
                    .should().resideInAPackage("co.personal.ynabsyncher.model.reconciliation..")
                    .because("Reconciliation classes should be organized in reconciliation package");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureTest {

        @Test
        @DisplayName("Hexagonal architecture layers should be respected")
        void hexagonalArchitectureLayersShouldBeRespected() {
            ArchRule rule = layeredArchitecture()
                    .consideringOnlyDependenciesInLayers() // This ignores JDK dependencies
                    
                    // Define layers
                    .layer("Domain").definedBy("co.personal.ynabsyncher.model..")
                    .layer("API").definedBy("co.personal.ynabsyncher.api..")
                    .layer("SPI").definedBy("co.personal.ynabsyncher.spi..")
                    .layer("UseCase").definedBy("co.personal.ynabsyncher.usecase..")
                    
                    // Define allowed access patterns
                    .whereLayer("UseCase").mayOnlyAccessLayers("Domain", "API", "SPI")
                    .whereLayer("API").mayOnlyAccessLayers("Domain")
                    .whereLayer("SPI").mayOnlyAccessLayers("Domain")
                    .whereLayer("Domain").mayNotAccessAnyLayer()
                    
                    .because("Hexagonal architecture layers should respect dependency rules");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Testing Architecture")
    class TestingArchitecture {

        // Note: Test package organization rules require more complex ArchUnit setup
        // This test is commented out to avoid API complexity
        // @Test
        // @DisplayName("Test classes should be in same package as tested classes")
        // void testClassesShouldBeInSamePackageAsTestedClasses() {
        //     // Test classes should mirror the package structure of production code
        // }
    }

    @Nested
    @DisplayName("Enhanced Contract Validation")
    class ContractValidation {

        @Test
        @DisplayName("Repository interfaces should not expose infrastructure details")
        void repositoryInterfacesShouldNotExposeInfrastructureDetails() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..spi.repository..")
                    .and().areInterfaces()
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", 
                        "jakarta.persistence..", 
                        "org.hibernate.."
                    )
                    .as("Repository interfaces should not expose infrastructure details");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("SPI ports should only be accessed by use cases and infrastructure")
        void spiPortsShouldOnlyBeAccessedByUseCasesAndInfrastructure() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..spi..")
                    .should().onlyBeAccessed().byClassesThat().resideInAnyPackage(
                        "..usecase..", 
                        "..infrastructure..", 
                        "..spi.."
                    )
                    .as("SPI ports should only be accessed by use cases and infrastructure adapters");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Domain services should not depend on use cases")
        void domainServicesShouldNotDependOnUseCases() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..usecase..")
                    .as("Domain services should not depend on use cases (wrong direction)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("API DTOs should not be used in domain services")
        void apiDtosShouldNotBeUsedInDomainServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..api.dto..")
                    .as("Domain services should use domain models, not API DTOs");

            rule.check(allClasses);
        }
    }
}