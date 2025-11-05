package co.personal.ynabsyncher.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import co.personal.ynabsyncher.ddd.DomainService;

/**
 * Spring configuration for domain layer components.
 * 
 * Uses component scanning to automatically discover and register domain services 
 * annotated with @DomainService. This maintains hexagonal architecture principles:
 * 
 * - Infrastructure → Domain dependency (allowed)
 * - Domain remains framework-free (only custom annotations)
 * - Automatic bean registration reduces boilerplate
 * - Clear architectural intent through @DomainService annotation
 */
@Configuration
@ComponentScan(
    basePackages = "co.personal.ynabsyncher.usecase", 
    includeFilters = {@ComponentScan.Filter(
        type = FilterType.ANNOTATION, 
        classes = DomainService.class
    )}
)
public class DomainConfig {
    
    /**
     * Component scanning automatically discovers:
     * - ImportBankTransactionsUseCase
     * - SaveCategoryMappingsUseCase  
     * - InferTransactionCategoriesUseCase
     * - ReconcileTransactionsUseCase
     * - CreateMissingTransactionsUseCase
     * 
     * All annotated with @DomainService and registered as Spring beans
     * with automatic constructor-based dependency injection.
     */
}
