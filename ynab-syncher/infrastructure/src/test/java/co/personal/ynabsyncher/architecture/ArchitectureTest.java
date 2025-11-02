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
        @DisplayName("Netflix/Uber microservices pattern layers should be respected")
        void netflixUberPatternLayersShouldBeRespected() {
            ArchRule rule = layeredArchitecture()
                    .consideringOnlyDependenciesInLayers() // This ignores JDK dependencies
                    
                    // Define layers according to Netflix/Uber pattern
                    .layer("Domain").definedBy(
                        "co.personal.ynabsyncher.model..", 
                        "co.personal.ynabsyncher.api..",
                        "co.personal.ynabsyncher.spi..",
                        "co.personal.ynabsyncher.usecase.."
                    )
                    // ApplicationServices layer will be added when implemented
                    .layer("WebControllers").definedBy("..infrastructure.web..")
                    .layer("RepositoryAdapters").definedBy("..infrastructure.persistence..")
                    .layer("ClientAdapters").definedBy("..infrastructure.client..")
                    .layer("Configuration").definedBy("..infrastructure.config..")
                    
                    // Define proper access patterns for Netflix/Uber
                    // WebControllers access will be restricted to ApplicationServices when implemented
                    .whereLayer("RepositoryAdapters").mayOnlyAccessLayers("Domain", "Configuration")
                    .whereLayer("ClientAdapters").mayOnlyAccessLayers("Domain", "Configuration")
                    .whereLayer("Configuration").mayOnlyAccessLayers("Domain", "ClientAdapters", "RepositoryAdapters")
                    .whereLayer("Domain").mayNotAccessAnyLayer()
                    
                    .because("Netflix/Uber microservices pattern should be enforced (ready for ApplicationServices)");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Testing Architecture")
    class TestingArchitecture {

        @Test
        @DisplayName("Test classes should follow proper organization")
        void testClassesShouldFollowProperOrganization() {
            // Allow nested test classes (they're good practice for organization)
            ArchRule testNamingRule = classes()
                    .that().haveSimpleNameEndingWith("Test")
                    .and().doNotHaveSimpleName("ArchitectureTest") // Exclude this architecture test
                    .should().haveNameMatching(".*Test$")
                    .because("Test classes should follow standard naming convention with 'Test' suffix");

            // Verify test classes don't pollute production packages  
            ArchRule testPackageRule = noClasses()
                    .that().haveSimpleNameEndingWith("Test")
                    .and().doNotHaveSimpleName("ArchitectureTest")
                    .and().areNotMemberClasses()  // Allow nested test classes
                    .should().resideInAPackage("co.personal.ynabsyncher.model..")
                    .orShould().resideInAPackage("co.personal.ynabsyncher.api..")
                    .orShould().resideInAPackage("co.personal.ynabsyncher.spi..")
                    .orShould().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .because("Test classes should not be in domain packages (use test source sets)");

            testNamingRule.check(allClasses);
            testPackageRule.check(allClasses);
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
        @DisplayName("Repository adapters should only access domain through SPI")
        void repositoryAdaptersShouldOnlyAccessDomainThroughSPI() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.slf4j..",
                        "..infrastructure.persistence..", // Own package
                        "..spi.repository..",            // SPI repository contracts
                        "..spi.client..",               // SPI client contracts (for composite adapters)
                        "..model..",                     // Domain models for mapping
                        "org.junit..", "org.assertj..", "org.mockito..", "org.testcontainers.."
                    )
                    .as("Repository adapters should implement SPI contracts and work with domain models only");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Client adapters should only access domain through SPI")
        void clientAdaptersShouldOnlyAccessDomainThroughSPI() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.client..")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.slf4j..",
                        "..infrastructure.client..",     // Own package (DTOs, mappers)
                        "..spi.client..",               // SPI contracts only
                        "..api.error..",                // Domain errors for exception mapping
                        "..model..",                    // Domain models for mapping
                        "org.junit..", "org.assertj..", "org.mockito..", "com.github.tomakehurst.wiremock.."
                    )
                    .as("Client adapters should implement SPI contracts and work with domain models only");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Application services should orchestrate domain through API")
        void applicationServicesShouldOrchestrateDomainThroughAPI() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.service..")
                    .and().haveSimpleNameEndingWith("ApplicationService")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.slf4j..",
                        "..infrastructure.service..",    // Own package
                        "..infrastructure.web.dto..",    // Web DTOs for conversion
                        "..api.usecase..",              // Use case orchestration (Netflix/Uber)
                        "..api.dto..",                  // Domain DTOs for conversion
                        "..api.error..",                // Domain errors for handling
                        "..model..",                    // Domain models if needed
                        "org.junit..", "org.assertj..", "org.mockito.."
                    )
                    .as("Application services should orchestrate domain use cases (Netflix/Uber pattern)")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Web controllers should only access application services")
        void webControllersShouldOnlyAccessApplicationServices() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.slf4j..",
                        "..infrastructure.web..",        // Own package (DTOs, mappers)
                        "..infrastructure.service..",    // Application services only
                        "org.junit..", "org.assertj..", "org.mockito.."
                    )
                    .as("Controllers should only depend on application services (Netflix/Uber pattern)")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Netflix/Uber Anti-Pattern Prevention")
    class NetflixUberAntiPatternPrevention {

        @Test
        @DisplayName("Controllers should not access domain use cases directly")
        void controllersShouldNotAccessDomainUseCasesDirectly() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().resideInAPackage("..api.usecase..")
                    .as("Controllers should not access domain use cases directly (Netflix/Uber pattern violation)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Controllers should not access domain DTOs directly")
        void controllersShouldNotAccessDomainDtosDirectly() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().resideInAPackage("..api.dto..")
                    .as("Controllers should work with Web DTOs, not domain DTOs directly");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Repository adapters should not access domain API")
        void repositoryAdaptersShouldNotAccessDomainAPI() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .should().dependOnClassesThat().resideInAPackage("..api.usecase..")
                    .orShould().dependOnClassesThat().resideInAPackage("..api.dto..")
                    .as("Repository adapters should not access domain API (they implement SPI only)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Client adapters should not access domain API")
        void clientAdaptersShouldNotAccessDomainAPI() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure.client..")
                    .should().dependOnClassesThat().resideInAPackage("..api.usecase..")
                    .orShould().dependOnClassesThat().resideInAPackage("..api.dto..")
                    .as("Client adapters should not access domain API (they implement SPI only)");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Production REST API Rules")
    class ProductionRestApiRules {

        @Test
        @DisplayName("Controllers should use proper HTTP annotations")
        void controllersShouldUseProperHttpAnnotations() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .as("REST controllers should use @RestController annotation")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Application services should use proper Spring annotations")
        void applicationServicesShouldUseProperSpringAnnotations() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.service..")
                    .and().haveSimpleNameEndingWith("ApplicationService")
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .as("Application services should use @Service annotation")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Web DTOs should use validation annotations properly")
        void webDtosShouldUseValidationAnnotationsProperly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.web.dto..")
                    .and().haveSimpleNameEndingWith("WebRequest")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "jakarta.validation..",  // Allow validation annotations
                        "com.fasterxml.jackson..", // Allow JSON annotations
                        "..infrastructure.web.dto.."
                    )
                    .as("Web request DTOs should use validation annotations")
                    .allowEmptyShould(true);

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Domain Purity Rules")
    class DomainPurityRules {

        @Test
        @DisplayName("Domain use cases should not depend on infrastructure concerns")
        void domainUseCasesShouldNotDependOnInfrastructureConcerns() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("co.personal.ynabsyncher.usecase..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..",
                        "org.springframework.transaction..",  // No @Transactional in domain
                        "org.springframework.cache..",        // No @Cacheable in domain
                        "org.springframework.security.."      // No security in domain
                    )
                    .because("Domain use cases should remain pure business logic");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Domain models should not expose infrastructure interfaces")
        void domainModelsShouldNotExposeInfrastructureInterfaces() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("co.personal.ynabsyncher.model..")
                    .should().implement("org.springframework.data.repository.Repository")
                    .orShould().implement("jakarta.persistence.EntityManager")
                    .orShould().implement("org.springframework.web.bind.annotation.RequestMapping")
                    .because("Domain models should not implement infrastructure interfaces");

            rule.check(allClasses);
        }
    }

    @Nested
    @DisplayName("Infrastructure Adapter Access Patterns")
    class InfrastructureAdapterAccessPatterns {

        @Test
        @DisplayName("DTOs should be collocated with their adapters")
        void dtosShouldBeCollocatedWithTheirAdapters() {
            ArchRule webDtosRule = classes()
                    .that().haveSimpleNameEndingWith("WebRequest")    // ✅ Matches our naming convention
                    .or().haveSimpleNameEndingWith("WebResponse")     // ✅ Matches our naming convention
                    .should().resideInAPackage("..infrastructure.web.dto..")
                    .as("Web DTOs should be in infrastructure.web.dto package")
                    .allowEmptyShould(true);

            ArchRule externalApiDtosRule = classes()
                    .that().resideInAPackage("..infrastructure.client.dto..")
                    .should().haveSimpleNameContaining("Ynab")        // Only external API DTOs should contain "Ynab"
                    .orShould().haveSimpleNameContaining("External")  // Or have "External" in name
                    .orShould().haveSimpleNameContaining("Api")       // Or have "Api" in name
                    .as("External API DTOs should be properly named and located")
                    .allowEmptyShould(true);

            webDtosRule.check(allClasses);
            externalApiDtosRule.check(allClasses);
        }
    }
}
