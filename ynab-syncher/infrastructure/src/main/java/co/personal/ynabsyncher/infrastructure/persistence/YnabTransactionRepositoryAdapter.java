package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.spi.client.YnabApiClient;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository adapter that implements YnabTransactionRepository using the YNAB API client.
 * Bridges the domain SPI contract to the infrastructure API client implementation.
 */
@Repository
public class YnabTransactionRepositoryAdapter implements YnabTransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(YnabTransactionRepositoryAdapter.class);
    
    private final YnabApiClient ynabApiClient;
    private final String defaultBudgetId;

    public YnabTransactionRepositoryAdapter(
            YnabApiClient ynabApiClient,
            @Value("${ynab.api.default-budget-id:}") String defaultBudgetId) {
        this.ynabApiClient = ynabApiClient;
        this.defaultBudgetId = defaultBudgetId;
    }

    @Override
    public List<YnabTransaction> findByAccountIdAndDateRange(AccountId accountId, LocalDate fromDate, LocalDate toDate) {
        logger.debug("Fetching YNAB transactions for account: {} from {} to {} in budget: {}", 
                accountId.value(), fromDate, toDate, defaultBudgetId);
        
        String budgetId = determineBudgetId();
        
        // YNAB API doesn't support date range filtering directly, so we fetch all and filter
        List<YnabTransaction> allTransactions = ynabApiClient.getAccountTransactions(budgetId, accountId.value());
        
        return allTransactions.stream()
                .filter(transaction -> !transaction.date().isBefore(fromDate) && !transaction.date().isAfter(toDate))
                .toList();
    }

    @Override
    public List<YnabTransaction> findByAccountId(AccountId accountId) {
        logger.debug("Fetching all YNAB transactions for account: {} in budget: {}", accountId.value(), defaultBudgetId);
        
        String budgetId = determineBudgetId();
        return ynabApiClient.getAccountTransactions(budgetId, accountId.value());
    }

    private String determineBudgetId() {
        if (defaultBudgetId != null && !defaultBudgetId.isBlank()) {
            return defaultBudgetId;
        }
        
        // Fall back to first available budget
        return ynabApiClient.getBudgets().stream()
                .findFirst()
                .map(budget -> budget.id())
                .orElseThrow(() -> new IllegalStateException("No YNAB budgets available"));
    }
}