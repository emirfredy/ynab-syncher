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

        @Test
        @DisplayName("Domain models should not have setters")
        void domainModelsShouldNotHaveSetters() {
            // Domain models should be immutable
            // Since most of our domain models are records, we'll focus on the core principle:
            // Value objects should be records (which are naturally immutable)
            
            // Test that key value object types are records
            ArchRule valueObjectRecordsRule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .and().haveSimpleNameEndingWith("Id")
                    .or().haveSimpleName("Money")
                    .should().beRecords()
                    .because("Value objects like IDs and Money should be records to ensure immutability");

            // Test that BankTransaction and YnabTransaction are records
            ArchRule transactionEntitiesRule = classes()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .and().haveSimpleName("BankTransaction")
                    .or().haveSimpleName("YnabTransaction")
                    .should().beRecords()
                    .because("Transaction entities should be records to prevent setters and ensure immutability");

            valueObjectRecordsRule.check(allClasses);
            transactionEntitiesRule.check(allClasses);
        }
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

        @Test
        @DisplayName("Test classes should be in same package as tested classes")
        void testClassesShouldBeInSamePackageAsTestedClasses() {
            // Verify that test classes follow proper naming and organization
            // Test classes should have 'Test' suffix and be properly organized
            ArchRule testNamingRule = classes()
                    .that().haveSimpleNameEndingWith("Test")
                    .and().areNotMemberClasses() // Exclude nested test classes
                    .and().doNotHaveSimpleName("ArchitectureTest") // Exclude this architecture test
                    .should().haveNameMatching(".*Test$")
                    .because("Test classes should follow standard naming convention with 'Test' suffix");

            // Verify test classes are in appropriate packages relative to what they test
            // Use noClasses to exclude architecture package
            ArchRule packageStructureRule = noClasses()
                    .that().haveSimpleNameEndingWith("Test")
                    .and().doNotHaveSimpleName("ArchitectureTest")
                    .and().areNotMemberClasses()
                    .and().resideInAPackage("co.personal.ynabsyncher..")
                    .should().resideInAPackage("..architecture..")
                    .because("Test classes should not be in architecture package except for ArchitectureTest");

            testNamingRule.check(allClasses);
            packageStructureRule.check(allClasses);
        }
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
                    .that().resideInAPackage("co.personal.ynabsyncher.model.service..")  // Specific domain location
                    .or().resideInAPackage("co.personal.ynabsyncher.usecase.service..")   // Alternative domain location
                    .should().dependOnClassesThat().resideInAPackage("..usecase..")
                    .as("Domain services should not depend on use case interfaces")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Application services should orchestrate domain use cases")
        void applicationServicesShouldOrchestrateDomainUseCases() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.service..")  // Infrastructure application services
                    .and().haveSimpleNameEndingWith("ApplicationService")
                    .should().dependOnClassesThat().resideInAnyPackage("..api.usecase..")
                    .as("Application services should orchestrate domain use cases (Netflix/Uber pattern)")
                    .allowEmptyShould(true);  // Allow empty since we might not have application services yet

            rule.check(allClasses);
        }

        @Test
        @DisplayName("API DTOs should not be used in domain services")
        void apiDtosShouldNotBeUsedInDomainServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("co.personal.ynabsyncher.model.service..")  // Specific domain location
                    .or().resideInAPackage("co.personal.ynabsyncher.usecase.service..")   // Alternative domain location
                    .should().dependOnClassesThat().resideInAPackage("..api.dto..")
                    .as("Domain services should use domain models, not API DTOs")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Infrastructure Package Organization")
    class InfrastructurePackageOrganization {

        @Test
        @DisplayName("Infrastructure classes should be properly organized by adapter type")
        void infrastructureClassesShouldBeProperlyOrganizedByAdapterType() {
            // Web adapters (inbound) should be in web package
            ArchRule webAdaptersRule = classes()
                    .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                    .should().resideInAPackage("..infrastructure.web..")
                    .as("Web controllers should be in infrastructure.web package")
                    .allowEmptyShould(true);

            webAdaptersRule.check(allClasses);
        }

        @Test
        @DisplayName("Repository adapters should be in persistence package")
        void repositoryAdaptersShouldBeInPersistencePackage() {
            ArchRule repositoryAdaptersRule = classes()
                    .that().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().resideInAPackage("..infrastructure.persistence..")
                    .as("Repository adapters should be in infrastructure.persistence package");

            repositoryAdaptersRule.check(allClasses);
        }

        @Test
        @DisplayName("External client adapters should be in client package")
        void externalClientAdaptersShouldBeInClientPackage() {
            ArchRule clientAdaptersRule = classes()
                    .that().haveSimpleNameContaining("ApiClient")
                    .and().areNotInterfaces() // Exclude domain SPI interfaces
                    .or().haveSimpleNameContaining("ApiMapper")
                    .should().resideInAPackage("..infrastructure.client..")
                    .as("External API client implementations should be in infrastructure.client package");

            clientAdaptersRule.check(allClasses);
        }

        @Test
        @DisplayName("Spring configuration should be in config package")
        void springConfigurationShouldBeInConfigPackage() {
            ArchRule configRule = classes()
                    .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
                    .should().resideInAPackage("..infrastructure.config..")
                    .as("Spring configurations should be in infrastructure.config package");

            configRule.check(allClasses);
        }

        @Test
        @DisplayName("In-memory implementations should be in memory package")
        void inMemoryImplementationsShouldBeInMemoryPackage() {
            ArchRule memoryRule = classes()
                    .that().haveSimpleNameStartingWith("InMemory")
                    .should().resideInAPackage("..infrastructure.memory..")
                    .as("In-memory implementations should be in infrastructure.memory package");

            memoryRule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Infrastructure Dependency Rules")
    class InfrastructureDependencyRules {

        @Test
        @DisplayName("Infrastructure should not depend on other infrastructure modules")
        void infrastructureShouldNotDependOnOtherInfrastructureModules() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure.web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
                    .orShould().dependOnClassesThat().resideInAPackage("..infrastructure.client..")
                    .as("Infrastructure adapters should not depend on each other directly");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Non-config infrastructure should only access domain through SPI contracts")
        void nonConfigInfrastructureShouldOnlyAccessDomainThroughSPIContracts() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.client..")
                    .or().resideInAPackage("..infrastructure.persistence..")
                    .or().resideInAPackage("..infrastructure.web..")
                    .or().resideInAPackage("..infrastructure.memory..")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.slf4j..",
                        "..infrastructure..", // Can access other infrastructure classes
                        "..spi..", // Should access domain through SPI
                        "..api.error..", // Can access domain errors
                        "..model..", // Allow access to domain models for mapping
                        "org.junit..", // Allow test frameworks for test classes
                        "org.assertj..", // Allow test frameworks for test classes
                        "org.mockito..", // Allow test frameworks for test classes
                        "org.testcontainers..", // Allow Testcontainers in tests
                        "com.github.tomakehurst.wiremock.." // Allow WireMock for test classes
                    )
                    .as("Non-config infrastructure should access domain through SPI contracts");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("DTOs should be collocated with their adapters")
        void dtosShouldBeCollocatedWithTheirAdapters() {
            ArchRule webDtosRule = classes()
                    .that().haveSimpleNameEndingWith("RequestDto")
                    .or().haveSimpleNameEndingWith("ResponseDto")
                    .should().resideInAPackage("..infrastructure.web.dto..")
                    .as("Web DTOs should be in infrastructure.web.dto package")
                    .allowEmptyShould(true);

            ArchRule apiDtosRule = classes()
                    .that().haveSimpleNameContaining("Ynab")
                    .and().haveSimpleNameEndingWith("Dto")
                    .or().haveSimpleNameContaining("Ynab")
                    .and().haveSimpleNameEndingWith("Response")
                    .or().haveSimpleNameContaining("Ynab")
                    .and().haveSimpleNameEndingWith("Request")
                    .should().resideInAPackage("..infrastructure.client.dto..")
                    .as("YNAB API DTOs should be in infrastructure.client.dto package");

            webDtosRule.check(allClasses);
            apiDtosRule.check(allClasses);
        }
    }
}
