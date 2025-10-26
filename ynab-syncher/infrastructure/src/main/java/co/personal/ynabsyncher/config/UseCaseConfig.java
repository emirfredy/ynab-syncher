package co.personal.ynabsyncher.config;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.service.TransactionReconciliationService;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import co.personal.ynabsyncher.usecase.ReconcileTransactionsUseCase;
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
     * Creates the main reconciliation use case.
     * 
     * This use case orchestrates the reconciliation workflow by delegating
     * the complex matching logic to the domain service while managing
     * repository interactions and result composition.
     */
    @Bean
    public ReconcileTransactions reconcileTransactions(
            YnabTransactionRepository ynabTransactionRepository,
            BankTransactionRepository bankTransactionRepository,
            TransactionReconciliationService reconciliationService) {
        return new ReconcileTransactionsUseCase(
            ynabTransactionRepository,
            bankTransactionRepository,
            reconciliationService
        );
    }
}