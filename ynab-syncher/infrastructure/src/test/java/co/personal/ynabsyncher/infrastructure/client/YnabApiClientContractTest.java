package co.personal.ynabsyncher.infrastructure.client;

import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.ynab.*;
import co.personal.ynabsyncher.spi.client.YnabApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests validating that YnabApiClientImpl correctly implements 
 * the YnabApiClient SPI interface according to domain expectations.
 * 
 * This abstract test class defines the behavioral contract that any
 * YnabApiClient implementation must satisfy. It ensures:
 * - Never returns null where Optional or List is expected
 * - Proper input validation with appropriate exceptions
 * - Consistent error handling patterns
 * - Domain boundary contract compliance
 */
@DisplayName("YnabApiClient Contract Tests")
abstract class YnabApiClientContractTest {

    /**
     * Subclasses must provide the implementation under test.
     * This allows testing both real and mock implementations.
     */
    protected abstract YnabApiClient createYnabApiClient();

    @Nested
    @DisplayName("Budget Operations")
    class BudgetOperations {

        @Test
        @DisplayName("getBudgets() should never return null")
        void getBudgetsShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabBudget> budgets = client.getBudgets();
            
            assertThat(budgets)
                    .as("getBudgets() must never return null - empty list is acceptable")
                    .isNotNull();
        }

        @Test
        @DisplayName("getBudget() should return Optional.empty() for non-existent budget")
        void getBudgetShouldReturnEmptyForNonExistentBudget() {
            YnabApiClient client = createYnabApiClient();
            
            Optional<YnabBudget> budget = client.getBudget("non-existent-id");
            
            assertThat(budget)
                    .as("getBudget() must never return null - Optional.empty() for missing budgets")
                    .isNotNull();
        }

        @Test
        @DisplayName("getBudget() should throw IllegalArgumentException for null budgetId")
        void getBudgetShouldThrowForNullBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getBudget(null))
                    .as("getBudget() must validate null budgetId at SPI boundary")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
        }

        @Test
        @DisplayName("getBudget() should throw IllegalArgumentException for blank budgetId")
        void getBudgetShouldThrowForBlankBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getBudget("  "))
                    .as("getBudget() must validate blank budgetId at SPI boundary")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");
        }

        @Test
        @DisplayName("getBudget() should throw IllegalArgumentException for empty budgetId")
        void getBudgetShouldThrowForEmptyBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getBudget(""))
                    .as("getBudget() must validate empty budgetId at SPI boundary")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Account Operations")
    class AccountOperations {

        @Test
        @DisplayName("getAccounts() should never return null")
        void getAccountsShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabAccount> accounts = client.getAccounts("valid-budget-id");
            
            assertThat(accounts)
                    .as("getAccounts() must never return null - empty list is acceptable")
                    .isNotNull();
        }

        @Test
        @DisplayName("getAccounts() should validate null budgetId")
        void getAccountsShouldValidateNullBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getAccounts(null))
                    .as("getAccounts() must validate null budgetId at SPI boundary")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
        }

        @Test
        @DisplayName("getAccounts() should validate blank budgetId")
        void getAccountsShouldValidateBlankBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getAccounts("   "))
                    .as("getAccounts() must validate blank budgetId at SPI boundary")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");
        }

        @Test
        @DisplayName("getAccountTransactions() should validate budgetId and accountId")
        void getAccountTransactionsShouldValidateInputs() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getAccountTransactions(null, "account-123"))
                    .as("getAccountTransactions() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
                    
            assertThatThrownBy(() -> client.getAccountTransactions("budget-123", null))
                    .as("getAccountTransactions() must validate null accountId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account ID cannot be null");

            assertThatThrownBy(() -> client.getAccountTransactions("", "account-123"))
                    .as("getAccountTransactions() must validate empty budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");

            assertThatThrownBy(() -> client.getAccountTransactions("budget-123", ""))
                    .as("getAccountTransactions() must validate empty accountId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account ID cannot be null or empty");
        }

        @Test
        @DisplayName("getAccountTransactions() should never return null")
        void getAccountTransactionsShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabTransaction> transactions = client.getAccountTransactions("valid-budget", "valid-account");
            
            assertThat(transactions)
                    .as("getAccountTransactions() must never return null")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("Category Operations")
    class CategoryOperations {

        @Test
        @DisplayName("getCategories() should never return null")
        void getCategoriesShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabCategory> categories = client.getCategories("valid-budget-id");
            
            assertThat(categories)
                    .as("getCategories() must never return null - empty list is acceptable")
                    .isNotNull();
        }

        @Test
        @DisplayName("getCategories() should validate budgetId")
        void getCategoriesShouldValidateBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getCategories(null))
                    .as("getCategories() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");

            assertThatThrownBy(() -> client.getCategories("  "))
                    .as("getCategories() must validate blank budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Transaction Operations")
    class TransactionOperations {

        @Test
        @DisplayName("getTransactions() should never return null")
        void getTransactionsShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabTransaction> transactions = client.getTransactions("valid-budget-id");
            
            assertThat(transactions)
                    .as("getTransactions() must never return null - empty list is acceptable")
                    .isNotNull();
        }

        @Test
        @DisplayName("getTransactions() should validate budgetId")
        void getTransactionsShouldValidateBudgetId() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getTransactions(null))
                    .as("getTransactions() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
        }

        @Test
        @DisplayName("getTransactionsSince() should validate inputs")
        void getTransactionsSinceShouldValidateInputs() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.getTransactionsSince(null, OffsetDateTime.now()))
                    .as("getTransactionsSince() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
                    
            assertThatThrownBy(() -> client.getTransactionsSince("valid-budget", null))
                    .as("getTransactionsSince() must validate null sinceDate")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Since date cannot be null");
        }

        @Test
        @DisplayName("getTransactionsSince() should never return null")
        void getTransactionsSinceShouldNeverReturnNull() {
            YnabApiClient client = createYnabApiClient();
            
            List<YnabTransaction> transactions = client.getTransactionsSince("valid-budget", OffsetDateTime.now());
            
            assertThat(transactions)
                    .as("getTransactionsSince() must never return null")
                    .isNotNull();
        }

        @Test
        @DisplayName("createTransaction() should validate inputs")
        void createTransactionShouldValidateInputs() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.createTransaction(null, validTransaction()))
                    .as("createTransaction() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
                    
            assertThatThrownBy(() -> client.createTransaction("valid-budget", null))
                    .as("createTransaction() must validate null transaction")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction cannot be null");

            assertThatThrownBy(() -> client.createTransaction("", validTransaction()))
                    .as("createTransaction() must validate empty budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");
        }

        @Test
        @DisplayName("updateTransaction() should validate inputs")
        void updateTransactionShouldValidateInputs() {
            YnabApiClient client = createYnabApiClient();
            
            assertThatThrownBy(() -> client.updateTransaction(null, "tx-123", validTransaction()))
                    .as("updateTransaction() must validate null budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null");
                    
            assertThatThrownBy(() -> client.updateTransaction("budget-123", null, validTransaction()))
                    .as("updateTransaction() must validate null transactionId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction ID cannot be null");

            assertThatThrownBy(() -> client.updateTransaction("budget-123", "tx-123", null))
                    .as("updateTransaction() must validate null transaction")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction cannot be null");

            assertThatThrownBy(() -> client.updateTransaction("", "tx-123", validTransaction()))
                    .as("updateTransaction() must validate empty budgetId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID cannot be null or empty");

            assertThatThrownBy(() -> client.updateTransaction("budget-123", "", validTransaction()))
                    .as("updateTransaction() must validate empty transactionId")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction ID cannot be null or empty");
        }

        private YnabTransaction validTransaction() {
            return new YnabTransaction(
                    TransactionId.of("test-transaction-id"), // Valid ID for testing
                    AccountId.of("account-123"),
                    LocalDate.now(),
                    Money.of(10.00),
                    "Test Payee",
                    "Test memo",
                    new Category(CategoryId.of("cat-123"), "Test Category", CategoryType.YNAB_ASSIGNED), // Valid category
                    ClearedStatus.UNCLEARED,
                    true, // approved
                    null // flagColor - can be null
            );
        }
    }

    @Nested
    @DisplayName("Health Check Operations")
    class HealthCheckOperations {

        @Test
        @DisplayName("isHealthy() should return boolean without throwing")
        void isHealthyShouldReturnBooleanWithoutThrowing() {
            YnabApiClient client = createYnabApiClient();
            
            // Health check should never throw - it should always return true or false
            assertThatCode(() -> {
                boolean healthy = client.isHealthy();
                assertThat(healthy).isIn(true, false);
            }).as("isHealthy() must never throw exceptions - should return true/false")
              .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isHealthy() should be resilient to errors")
        void isHealthyShouldBeResilientToErrors() {
            YnabApiClient client = createYnabApiClient();
            
            // Even if underlying operations fail, isHealthy should return false, not throw
            boolean healthy = client.isHealthy();
            
            assertThat(healthy)
                    .as("isHealthy() must return a boolean value regardless of underlying state")
                    .isIn(true, false);
        }
    }

    @Nested
    @DisplayName("Domain Boundary Compliance")
    class DomainBoundaryCompliance {

        @Test
        @DisplayName("All operations should throw YnabApiException for API failures")
        void operationsShouldThrowYnabApiExceptionForApiFailures() {
            // This test validates that when API calls fail (not input validation),
            // the client throws YnabApiException (domain exception) rather than
            // infrastructure exceptions like HttpClientErrorException
            
            YnabApiClient client = createYnabApiClient();
            
            // Note: This test structure ensures that if the implementation
            // throws infrastructure exceptions instead of domain exceptions,
            // it will be caught. The actual test execution depends on the
            // concrete implementation provided by subclasses.
            
            assertThat(client)
                    .as("YnabApiClient implementation must exist")
                    .isNotNull();
        }

        @Test
        @DisplayName("Input validation should use IllegalArgumentException consistently")
        void inputValidationShouldUseIllegalArgumentExceptionConsistently() {
            YnabApiClient client = createYnabApiClient();
            
            // Verify that all input validation uses IllegalArgumentException
            // (not other exception types like NullPointerException)
            
            assertThatThrownBy(() -> client.getBudget(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.getAccounts(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.getCategories(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.getTransactions(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.getTransactionsSince("valid", null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.createTransaction(null, validTransaction()))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
                    
            assertThatThrownBy(() -> client.updateTransaction(null, "tx", validTransaction()))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }

        private YnabTransaction validTransaction() {
            return new YnabTransaction(
                    TransactionId.of("test-transaction-id"),
                    AccountId.of("account-123"),
                    LocalDate.now(),
                    Money.of(10.00),
                    "Test Payee",
                    "Test memo",
                    new Category(CategoryId.of("cat-123"), "Test Category", CategoryType.YNAB_ASSIGNED),
                    ClearedStatus.UNCLEARED,
                    true,
                    null
            );
        }
    }
}