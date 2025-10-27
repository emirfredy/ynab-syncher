package co.personal.ynabsyncher.config;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.api.usecase.InferTransactionCategories;
import co.personal.ynabsyncher.persistence.InMemoryCategoryMappingRepository;
import co.personal.ynabsyncher.service.TransactionReconciliationService;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import co.personal.ynabsyncher.service.CategoryInferenceService;
import co.personal.ynabsyncher.usecase.ReconcileTransactionsUseCase;
import co.personal.ynabsyncher.usecase.InferTransactionCategoriesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for use case beans and domain services.
 * 
 * This class wires together the application's use cases with their dependencies,
 * following the hexagonal architecture pattern where use cases orchestrate
 * domain services and repository ports.
 */
@Configuration
public class UseCaseConfig {

    /**
     * Creates the transaction reconciliation domain service.
     * 
     * This service encapsulates the complex matching algorithm logic,
     * providing optimized O(n×log(m)+n×k) performance vs the previous O(n×m).
     */
    @Bean
    public TransactionReconciliationService transactionReconciliationService() {
        return new TransactionReconciliationService();
    }

    /**
     * Creates the category inference domain service.
     * 
     * This service handles automatic category assignment for bank transactions
     * using fuzzy matching algorithms and confidence scoring.
     * Service is now framework-free and doesn't access repositories directly.
     */
    @Bean
    public CategoryInferenceService categoryInferenceService() {
        return new CategoryInferenceService();
    }

    /**
     * Creates the category mapping repository.
     * 
     * In-memory implementation for testing and initial development.
     * In production, this would be replaced with a database implementation.
     */
    @Bean
    public CategoryMappingRepository categoryMappingRepository() {
        return new InMemoryCategoryMappingRepository();
    }

    /**
     * Creates the main reconciliation use case.
     * 
     * This use case orchestrates the reconciliation workflow by delegating
     * the complex matching logic to the domain service while managing
     * repository interactions and result composition. Now includes category
     * inference for uncategorized bank transactions.
     */
    @Bean
    public ReconcileTransactions reconcileTransactions(
            YnabTransactionRepository ynabTransactionRepository,
            BankTransactionRepository bankTransactionRepository,
            YnabCategoryRepository ynabCategoryRepository,
            TransactionReconciliationService reconciliationService,
            CategoryInferenceService categoryInferenceService) {
        return new ReconcileTransactionsUseCase(
            ynabTransactionRepository,
            bankTransactionRepository,
            ynabCategoryRepository,
            reconciliationService,
            categoryInferenceService
        );
    }

    /**
     * Creates the category inference use case.
     * 
     * This use case provides transaction category inference capabilities
     * as a standalone service for API endpoints. Now supports learned mappings
     * for improved accuracy over time.
     */
    @Bean
    public InferTransactionCategories inferTransactionCategories(
            BankTransactionRepository bankTransactionRepository,
            YnabCategoryRepository ynabCategoryRepository,
            CategoryMappingRepository categoryMappingRepository,
            CategoryInferenceService categoryInferenceService) {
        return new InferTransactionCategoriesUseCase(
            bankTransactionRepository,
            ynabCategoryRepository,
            categoryMappingRepository,
            categoryInferenceService
        );
    }
}