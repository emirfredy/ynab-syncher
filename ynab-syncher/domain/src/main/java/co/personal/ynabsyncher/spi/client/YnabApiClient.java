package co.personal.ynabsyncher.spi.client;

import co.personal.ynabsyncher.api.error.YnabApiException;
import co.personal.ynabsyncher.model.ynab.YnabBudget;
import co.personal.ynabsyncher.model.ynab.YnabAccount;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service Provider Interface (SPI) for interacting with the YNAB API.
 * 
 * This interface provides access to YNAB budgets, accounts, categories, and transactions
 * without exposing any framework-specific details to the domain layer.
 * 
 * Implementations must handle:
 * - Authentication via Bearer token
 * - Rate limiting (200 requests/hour per token)
 * - Data format conversion (milliunits, ISO 8601 dates)
 * - Error handling and retries
 * - Delta requests for efficient synchronization
 * 
 * All monetary amounts use YNAB's "milliunits" format where 1000 = $1.00
 * All dates use ISO 8601 format with timezone information
 */
public interface YnabApiClient {

    /**
     * Retrieves all available budgets for the authenticated user.
     * 
     * @return List of budgets accessible to the current user
     * @throws YnabApiException if the request fails or authentication is invalid
     */
    List<YnabBudget> getBudgets();

    /**
     * Retrieves a specific budget by its ID.
     * 
     * @param budgetId The unique identifier for the budget
     * @return Optional containing the budget if found, empty otherwise
     * @throws YnabApiException if the request fails
     * @throws IllegalArgumentException if budgetId is null or empty
     */
    Optional<YnabBudget> getBudget(String budgetId);

    /**
     * Retrieves all accounts within a specific budget.
     * 
     * @param budgetId The unique identifier for the budget
     * @return List of accounts within the budget
     * @throws YnabApiException if the request fails or budget is not found
     * @throws IllegalArgumentException if budgetId is null or empty
     */
    List<YnabAccount> getAccounts(String budgetId);

    /**
     * Retrieves all categories within a specific budget.
     * 
     * @param budgetId The unique identifier for the budget
     * @return List of categories within the budget
     * @throws YnabApiException if the request fails or budget is not found
     * @throws IllegalArgumentException if budgetId is null or empty
     */
    List<YnabCategory> getCategories(String budgetId);

    /**
     * Retrieves transactions from a specific budget.
     * 
     * @param budgetId The unique identifier for the budget
     * @return List of transactions within the budget
     * @throws YnabApiException if the request fails or budget is not found
     * @throws IllegalArgumentException if budgetId is null or empty
     */
    List<YnabTransaction> getTransactions(String budgetId);

    /**
     * Retrieves transactions from a specific budget modified since a given date.
     * This supports YNAB's delta requests for efficient synchronization.
     * 
     * @param budgetId The unique identifier for the budget
     * @param sinceDate Only return transactions modified on or after this date
     * @return List of transactions modified since the specified date
     * @throws YnabApiException if the request fails or budget is not found
     * @throws IllegalArgumentException if budgetId is null/empty or sinceDate is null
     */
    List<YnabTransaction> getTransactionsSince(String budgetId, OffsetDateTime sinceDate);

    /**
     * Retrieves transactions from a specific account within a budget.
     * 
     * @param budgetId The unique identifier for the budget
     * @param accountId The unique identifier for the account
     * @return List of transactions within the specified account
     * @throws YnabApiException if the request fails, budget not found, or account not found
     * @throws IllegalArgumentException if budgetId or accountId is null or empty
     */
    List<YnabTransaction> getAccountTransactions(String budgetId, String accountId);

    /**
     * Creates a new transaction in YNAB.
     * 
     * @param budgetId The unique identifier for the budget
     * @param transaction The transaction to create
     * @return The created transaction with YNAB-assigned ID and metadata
     * @throws YnabApiException if the request fails, validation errors occur, or budget not found
     * @throws IllegalArgumentException if budgetId is null/empty or transaction is null
     */
    YnabTransaction createTransaction(String budgetId, YnabTransaction transaction);

    /**
     * Updates an existing transaction in YNAB.
     * 
     * @param budgetId The unique identifier for the budget
     * @param transactionId The unique identifier for the transaction to update
     * @param transaction The updated transaction data
     * @return The updated transaction
     * @throws YnabApiException if the request fails, transaction not found, or budget not found
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    YnabTransaction updateTransaction(String budgetId, String transactionId, YnabTransaction transaction);

    /**
     * Performs a health check against the YNAB API to verify connectivity and authentication.
     * 
     * @return true if the API is accessible and authentication is valid, false otherwise
     */
    boolean isHealthy();
}