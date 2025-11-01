package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.spi.client.YnabApiClient;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository adapter that implements YnabCategoryRepository using the YNAB API client.
 * Bridges the domain SPI contract to the infrastructure API client implementation.
 */
@Repository
public class YnabCategoryRepositoryAdapter implements YnabCategoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(YnabCategoryRepositoryAdapter.class);
    
    private final YnabApiClient ynabApiClient;
    private final String defaultBudgetId;

    public YnabCategoryRepositoryAdapter(
            YnabApiClient ynabApiClient,
            @Value("${ynab.api.default-budget-id:}") String defaultBudgetId) {
        this.ynabApiClient = ynabApiClient;
        this.defaultBudgetId = defaultBudgetId;
    }

    @Override
    public List<YnabCategory> findAllAvailableCategories() {
        logger.debug("Fetching all available YNAB categories for budget: {}", defaultBudgetId);
        
        String budgetId = determineBudgetId();
        
        return ynabApiClient.getCategories(budgetId).stream()
                .filter(category -> !category.isHidden() && !category.isDeleted())
                .toList();
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