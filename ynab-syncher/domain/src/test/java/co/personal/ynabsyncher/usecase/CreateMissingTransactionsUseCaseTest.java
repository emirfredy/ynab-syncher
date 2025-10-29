package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.TransactionCreationResult;
import co.personal.ynabsyncher.api.error.YnabApiException;
import co.personal.ynabsyncher.api.usecase.CreateMissingTransactions;
import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.BudgetId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.spi.client.YnabApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Create Missing Transactions - Use Case Tests")
class CreateMissingTransactionsUseCaseTest {

    private CreateMissingTransactions createMissingTransactions;
    private TestYnabApiClient testYnabApiClient;

    private BudgetId budgetId;
    private AccountId bankAccountId;
    private AccountId ynabAccountId;

    @BeforeEach
    void setUp() {
        testYnabApiClient = new TestYnabApiClient();
        createMissingTransactions = new CreateMissingTransactionsUseCase(testYnabApiClient);
        budgetId = BudgetId.of("budget-123");
        bankAccountId = AccountId.of("bank-acc-456");
        ynabAccountId = AccountId.of("ynab-acc-789");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw exception when YnabApiClient is null")
        void shouldThrowExceptionWhenYnabApiClientIsNull() {
            assertThatThrownBy(() -> new CreateMissingTransactionsUseCase(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("YNAB API client cannot be null");
        }
    }

    @Nested
    @DisplayName("Successful Transaction Creation")
    class SuccessfulTransactionCreation {

        @Test
        @DisplayName("Should successfully create single missing transaction")
        void shouldSuccessfullyCreateSingleMissingTransaction() {
            // Given
            BankTransaction bankTransaction = createBankTransaction(
                    "txn-1",
                    LocalDate.of(2024, 1, 15),
                    Money.of(BigDecimal.valueOf(-25.50)),
                    "GROCERY STORE #123",
                    "ACME GROCERY",
                    "Weekly groceries"
            );

            CreateMissingTransactionsRequest request = new CreateMissingTransactionsRequest(
                    budgetId,
                    bankAccountId,
                    ynabAccountId,
                    List.of(bankTransaction)
            );

            // When
            CreateMissingTransactionsResponse response = createMissingTransactions.createMissingTransactions(request);

            // Then
            assertThat(response.totalProcessed()).isEqualTo(1);
            assertThat(response.successfullyCreated()).isEqualTo(1);
            assertThat(response.failed()).isEqualTo(0);
            assertThat(response.results()).hasSize(1);

            TransactionCreationResult result = response.results().get(0);
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.description()).isEqualTo("GROCERY STORE #123");
            assertThat(result.amount()).isEqualTo(new BigDecimal("-25.50"));
            assertThat(result.date()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.errorMessage()).isNull();

            // Verify the API was called
            assertThat(testYnabApiClient.getCreateTransactionCallCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should successfully create multiple missing transactions")
        void shouldSuccessfullyCreateMultipleMissingTransactions() {
            // Given
            BankTransaction bankTransaction1 = createBankTransaction(
                    "txn-1",
                    LocalDate.of(2024, 1, 15),
                    Money.of(BigDecimal.valueOf(-25.50)),
                    "GROCERY STORE #123",
                    "ACME GROCERY",
                    "Weekly groceries"
            );

            BankTransaction bankTransaction2 = createBankTransaction(
                    "txn-2",
                    LocalDate.of(2024, 1, 16),
                    Money.of(BigDecimal.valueOf(-4.99)),
                    "COFFEE SHOP",
                    "STARBUCKS",
                    "Morning coffee"
            );

            CreateMissingTransactionsRequest request = new CreateMissingTransactionsRequest(
                    budgetId,
                    bankAccountId,
                    ynabAccountId,
                    List.of(bankTransaction1, bankTransaction2)
            );

            // When
            CreateMissingTransactionsResponse response = createMissingTransactions.createMissingTransactions(request);

            // Then
            assertThat(response.totalProcessed()).isEqualTo(2);
            assertThat(response.successfullyCreated()).isEqualTo(2);
            assertThat(response.failed()).isEqualTo(0);
            assertThat(response.results()).hasSize(2);

            TransactionCreationResult result1 = response.results().get(0);
            assertThat(result1.wasSuccessful()).isTrue();
            assertThat(result1.description()).isEqualTo("GROCERY STORE #123");

            TransactionCreationResult result2 = response.results().get(1);
            assertThat(result2.wasSuccessful()).isTrue();
            assertThat(result2.description()).isEqualTo("COFFEE SHOP");

            assertThat(testYnabApiClient.getCreateTransactionCallCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Failed Transaction Creation")
    class FailedTransactionCreation {

        @Test
        @DisplayName("Should handle YNAB API exception gracefully")
        void shouldHandleYnabApiExceptionGracefully() {
            // Given
            testYnabApiClient.setShouldThrowException(true);
            testYnabApiClient.setExceptionToThrow(new YnabApiException("Budget not found", new RuntimeException()));

            BankTransaction bankTransaction = createBankTransaction(
                    "txn-1",
                    LocalDate.of(2024, 1, 15),
                    Money.of(BigDecimal.valueOf(-25.50)),
                    "GROCERY STORE #123",
                    "ACME GROCERY",
                    "Weekly groceries"
            );

            CreateMissingTransactionsRequest request = new CreateMissingTransactionsRequest(
                    budgetId,
                    bankAccountId,
                    ynabAccountId,
                    List.of(bankTransaction)
            );

            // When
            CreateMissingTransactionsResponse response = createMissingTransactions.createMissingTransactions(request);

            // Then
            assertThat(response.totalProcessed()).isEqualTo(1);
            assertThat(response.successfullyCreated()).isEqualTo(0);
            assertThat(response.failed()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);

            TransactionCreationResult result = response.results().get(0);
            assertThat(result.wasSuccessful()).isFalse();
            assertThat(result.description()).isEqualTo("GROCERY STORE #123");
            assertThat(result.amount()).isEqualTo(new BigDecimal("-25.50"));
            assertThat(result.date()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(result.errorMessage()).contains("Failed to create transaction: Budget not found");
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should throw exception when request is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> createMissingTransactions.createMissingTransactions(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Request cannot be null");
        }
    }

    // Helper methods
    private BankTransaction createBankTransaction(
            String id,
            LocalDate date,
            Money amount,
            String description,
            String merchantName,
            String memo
    ) {
        return new BankTransaction(
                TransactionId.of(id),
                bankAccountId,
                date,
                amount,
                description,
                merchantName,
                memo,
                "DEBIT",
                "REF-" + id,
                Category.inferredCategory("Unknown Category")
        );
    }

    // Test stub for YnabApiClient
    private static class TestYnabApiClient implements YnabApiClient {
        private int createTransactionCallCount = 0;
        private boolean shouldThrowException = false;
        private YnabApiException exceptionToThrow;

        @Override
        public YnabTransaction createTransaction(String budgetId, YnabTransaction transaction) {
            createTransactionCallCount++;
            
            if (shouldThrowException && exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            
            // Return a new transaction with an ID assigned
            return new YnabTransaction(
                    TransactionId.of("ynab-" + createTransactionCallCount),
                    transaction.accountId(),
                    transaction.date(),
                    transaction.amount(),
                    transaction.payeeName(),
                    transaction.memo(),
                    transaction.category(),
                    transaction.clearedStatus(),
                    transaction.approved(),
                    transaction.flagColor()
            );
        }

        public int getCreateTransactionCallCount() {
            return createTransactionCallCount;
        }

        public void setShouldThrowException(boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
        }

        public void setExceptionToThrow(YnabApiException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        // Stub implementations for other methods (not used in these tests)
        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabBudget> getBudgets() { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public java.util.Optional<co.personal.ynabsyncher.model.ynab.YnabBudget> getBudget(String budgetId) { 
            return java.util.Optional.empty(); 
        }

        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabAccount> getAccounts(String budgetId) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabCategory> getCategories(String budgetId) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabTransaction> getTransactions(String budgetId) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabTransaction> getTransactionsSince(String budgetId, java.time.OffsetDateTime sinceDate) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public List<co.personal.ynabsyncher.model.ynab.YnabTransaction> getAccountTransactions(String budgetId, String accountId) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public YnabTransaction updateTransaction(String budgetId, String transactionId, YnabTransaction transaction) { 
            return transaction; 
        }

        @Override
        public boolean isHealthy() { 
            return true; 
        }
    }
}