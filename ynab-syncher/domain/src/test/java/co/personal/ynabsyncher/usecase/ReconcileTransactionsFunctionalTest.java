package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Reconcile Transactions Use Case")
class ReconcileTransactionsFunctionalTest {

    @Mock
    private YnabTransactionRepository ynabTransactionRepository;
    
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    
    private ReconcileTransactions reconcileTransactions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reconcileTransactions = new ReconcileTransactionsUseCase(
            ynabTransactionRepository, 
            bankTransactionRepository
        );
    }

    @Test
    @DisplayName("Should identify missing transactions when bank has transactions not in YNAB")
    void shouldIdentifyMissingTransactions() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        Transaction ynabTransaction = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station",
            "Fuel"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingTransactionCount()).isEqualTo(1);
        assertThat(result.getMatchedTransactionCount()).isEqualTo(1);
        assertThat(result.missingFromYnab()).hasSize(1);
        assertThat(result.missingFromYnab().get(0).id()).isEqualTo(TransactionId.of("bank-2"));
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.summary().strategy()).isEqualTo(ReconciliationStrategy.STRICT);
    }

    @Test
    @DisplayName("Should return empty list when all bank transactions exist in YNAB")
    void shouldReturnEmptyListWhenAllTransactionsMatch() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station",
            "Fuel"
        );
        
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station",
            "Fuel"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);

        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));

        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));

        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingTransactionCount()).isEqualTo(0);
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.missingFromYnab()).isEmpty();
        assertThat(result.isFullyReconciled()).isTrue();
    }

    @Test
    @DisplayName("Should match transactions by amount and date")
    void shouldMatchTransactionsByAmountAndDate() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(123.45),
            "Store A",
            "Purchase"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.25),
            "Restaurant",
            "Dinner"
        );
        
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(123.45),
            "Store B", // Different payee name
            "Different memo"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.25),
            "Restaurant XYZ", // Different payee name
            "Meal"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.getMissingTransactionCount()).isEqualTo(0);
        assertThat(result.isFullyReconciled()).isTrue();
    }

    @Test
    @DisplayName("Should handle duplicate transactions correctly")
    void shouldHandleDuplicateTransactions() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate date = LocalDate.of(2024, 1, 15);
        Money amount = Money.of(50.00);
        
        // Two identical YNAB transactions
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase"
        );
        
        // Two identical bank transactions
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, date, date))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, date, date))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, date, date);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        // With current implementation, each bank transaction matches the first YNAB transaction
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.getMissingTransactionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reconcile transactions within a specific date range")
    void shouldReconcileTransactionsWithinDateRange() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 10);
        LocalDate toDate = LocalDate.of(2024, 1, 20);
        
        // Transactions within range
        Transaction ynabTransactionInRange1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase"
        );
        
        Transaction ynabTransactionInRange2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 18),
            Money.of(45.50),
            "Pharmacy",
            "Medicine"
        );
        
        Transaction bankTransactionInRange1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase"
        );
        
        Transaction bankTransactionInRange2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 18),
            Money.of(45.50),
            "Pharmacy",
            "Medicine"
        );
        
        // Additional bank transaction without YNAB match
        Transaction bankTransactionMissing = Transaction.bankTransaction(
            TransactionId.of("bank-3"),
            accountId,
            LocalDate.of(2024, 1, 12),
            Money.of(25.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransactionInRange1, ynabTransactionInRange2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransactionInRange1, bankTransactionInRange2, bankTransactionMissing));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.getMissingTransactionCount()).isEqualTo(1);
        assertThat(result.missingFromYnab()).hasSize(1);
        assertThat(result.missingFromYnab().get(0).id()).isEqualTo(TransactionId.of("bank-3"));
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.summary().fromDate()).isEqualTo(fromDate);
        assertThat(result.summary().toDate()).isEqualTo(toDate);
    }

    @Test
    @DisplayName("Should reconcile transactions for a specific account")
    void shouldReconcileTransactionsForSpecificAccount() {
        // Given
        AccountId targetAccountId = AccountId.of("target-account");
        LocalDate fromDate = LocalDate.of(2024, 1, 10);
        LocalDate toDate = LocalDate.of(2024, 1, 20);
        
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            targetAccountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            targetAccountId,
            LocalDate.of(2024, 1, 18),
            Money.of(65.75),
            "Gas Station",
            "Fuel"
        );
        
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            targetAccountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            targetAccountId,
            LocalDate.of(2024, 1, 18),
            Money.of(65.75),
            "Gas Station",
            "Fuel"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(targetAccountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(targetAccountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(targetAccountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.getMissingTransactionCount()).isEqualTo(0);
        assertThat(result.isFullyReconciled()).isTrue();
        assertThat(result.summary().accountId()).isEqualTo(targetAccountId);
    }

    @Test
    @DisplayName("Should match transactions within date range using RANGE strategy")
    void shouldMatchTransactionsWithinRangeStrategy() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        // YNAB transactions
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.50),
            "Restaurant",
            "Lunch"
        );
        
        // Bank transactions with slight date differences (should match with RANGE strategy)
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 17), // 2 days later, should match
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20), // 2 days earlier, should match
            Money.of(75.50),
            "Restaurant",
            "Lunch"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.RANGE);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingTransactionCount()).isEqualTo(0);
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2);
        assertThat(result.isFullyReconciled()).isTrue();
        assertThat(result.summary().strategy()).isEqualTo(ReconciliationStrategy.RANGE);
    }

    @Test
    @DisplayName("Should not match transactions beyond date range using RANGE strategy")
    void shouldNotMatchTransactionsBeyondRangeStrategy() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        // YNAB transactions
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 10),
            Money.of(85.75),
            "Bookstore",
            "Books"
        );
        
        // Bank transactions beyond the 3-day range (should NOT match with RANGE strategy)
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 20), // 5 days later, should NOT match
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 5), // 5 days earlier, should NOT match
            Money.of(85.75),
            "Bookstore",
            "Books"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.RANGE);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingTransactionCount()).isEqualTo(2);
        assertThat(result.getMatchedTransactionCount()).isEqualTo(0);
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.summary().strategy()).isEqualTo(ReconciliationStrategy.RANGE);
    }

    @Test
    @DisplayName("STRICT strategy should not match transactions with different dates")
    void strictStrategyShouldNotMatchDifferentDates() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        // YNAB transactions
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 25),
            Money.of(60.25),
            "Grocery Store",
            "Groceries"
        );
        
        // Bank transactions with different dates (should NOT match with STRICT strategy)
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 16), // 1 day later, should NOT match with STRICT
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 24), // 1 day earlier, should NOT match with STRICT
            Money.of(60.25),
            "Grocery Store",
            "Groceries"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingTransactionCount()).isEqualTo(2);
        assertThat(result.getMatchedTransactionCount()).isEqualTo(0);
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.summary().strategy()).isEqualTo(ReconciliationStrategy.STRICT);
    }

    @Test
    @DisplayName("Should handle complex mixed reconciliation scenario with multiple transactions")
    void shouldHandleComplexMixedReconciliationScenario() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        // YNAB transactions (3 total)
        Transaction ynabTransaction1 = Transaction.ynabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 5),
            Money.of(150.00),
            "Grocery Store",
            "Weekly groceries"
        );
        
        Transaction ynabTransaction2 = Transaction.ynabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(80.50),
            "Restaurant",
            "Lunch meeting"
        );
        
        Transaction ynabTransaction3 = Transaction.ynabTransaction(
            TransactionId.of("ynab-3"),
            accountId,
            LocalDate.of(2024, 1, 25),
            Money.of(45.75),
            "Bookstore",
            "Technical books"
        );
        
        // Bank transactions (4 total)
        Transaction bankTransaction1 = Transaction.bankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 5),
            Money.of(150.00),
            "Grocery Store XYZ",  // Matches ynab-1
            "Groceries"
        );
        
        Transaction bankTransaction2 = Transaction.bankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 10),
            Money.of(35.25),
            "Gas Station",        // No YNAB match
            "Fuel"
        );
        
        Transaction bankTransaction3 = Transaction.bankTransaction(
            TransactionId.of("bank-3"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(80.50),
            "Restaurant ABC",     // Matches ynab-2
            "Business lunch"
        );
        
        Transaction bankTransaction4 = Transaction.bankTransaction(
            TransactionId.of("bank-4"),
            accountId,
            LocalDate.of(2024, 1, 28),
            Money.of(22.99),
            "Coffee Shop",        // No YNAB match
            "Coffee and pastry"
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2, ynabTransaction3));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2, bankTransaction3, bankTransaction4));
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedTransactionCount()).isEqualTo(2); // bank-1 & bank-3 matched
        assertThat(result.getMissingTransactionCount()).isEqualTo(2); // bank-2 & bank-4 not in YNAB
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.missingFromYnab()).hasSize(2);
        
        // Verify specific missing transactions
        var missingIds = result.missingFromYnab().stream()
            .map(Transaction::id)
            .toList();
        assertThat(missingIds).containsExactlyInAnyOrder(
            TransactionId.of("bank-2"), 
            TransactionId.of("bank-4")
        );
        
        assertThat(result.summary().strategy()).isEqualTo(ReconciliationStrategy.STRICT);
        assertThat(result.summary().totalBankTransactions()).isEqualTo(4);
        assertThat(result.summary().totalYnabTransactions()).isEqualTo(3);
        assertThat(result.summary().matchedTransactions()).isEqualTo(2);
        assertThat(result.summary().missingFromYnab()).isEqualTo(2);
    }
}