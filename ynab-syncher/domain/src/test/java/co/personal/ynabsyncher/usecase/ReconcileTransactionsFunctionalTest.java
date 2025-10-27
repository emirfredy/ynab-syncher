package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.model.reconciliation.TransactionMatchResult;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import co.personal.ynabsyncher.service.TransactionReconciliationService;
import co.personal.ynabsyncher.service.CategoryInferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Reconcile Transactions Use Case")
class ReconcileTransactionsFunctionalTest {

    @Mock
    private YnabTransactionRepository ynabTransactionRepository;
    
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    
    @Mock
    private YnabCategoryRepository ynabCategoryRepository;
    
    @Mock
    private TransactionReconciliationService reconciliationService;
    
    @Mock
    private CategoryInferenceService categoryInferenceService;
    
    private ReconcileTransactions reconcileTransactions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reconcileTransactions = new ReconcileTransactionsUseCase(
            ynabTransactionRepository, 
            bankTransactionRepository,
            ynabCategoryRepository,
            reconciliationService,
            categoryInferenceService
        );
    }

    private void mockReconciliationService(List<BankTransaction> matched, List<BankTransaction> missing) {
        when(reconciliationService.reconcileTransactions(any(), any(), any()))
            .thenReturn(new TransactionMatchResult(matched, missing));
    }

    @Test
    @DisplayName("Should identify missing transactions when bank has transactions not in YNAB")
    void shouldIdentifyMissingTransactions() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);
        
        YnabTransaction ynabTransaction = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF123",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station Purchase",
            "Gas Station",
            "Fuel",
            "DEBIT",
            "REF124",
            Category.inferredCategory("Transportation")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1), // matched: bank-1 matches ynab-1
            List.of(bankTransaction2)   // missing: bank-2 not in YNAB
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isEqualTo(1);
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
        
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station",
            "Fuel",
            Category.ynabCategory("transport", "Transportation"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF123",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20),
            Money.of(50.00),
            "Gas Station Purchase",
            "Gas Station",
            "Fuel",
            "DEBIT",
            "REF124",
            Category.inferredCategory("Transportation")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction2), // all matched
            List.of()                                    // none missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingCount()).isEqualTo(0);
        assertThat(result.getMatchedCount()).isEqualTo(2);
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
        
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(123.45),
            "Store A",
            "Purchase",
            Category.ynabCategory("shopping", "Shopping"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.25),
            "Restaurant",
            "Dinner",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(123.45),
            "Store B Purchase", // Different payee name
            "Store B",
            "Different memo",
            "DEBIT",
            "REF125",
            Category.inferredCategory("Shopping")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.25),
            "Restaurant XYZ", // Different payee name
            "Restaurant XYZ",
            "Meal",
            "DEBIT",
            "REF126",
            Category.inferredCategory("Dining Out")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction2), // all matched by amount and date
            List.of()                                    // none missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedCount()).isEqualTo(2);
        assertThat(result.getMissingCount()).isEqualTo(0);
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
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase",
            Category.ynabCategory("shopping", "Shopping"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            date,
            amount,
            "Store",
            "Purchase",
            Category.ynabCategory("shopping", "Shopping"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        // Two identical bank transactions
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            date,
            amount,
            "Store Purchase",
            "Store",
            "Purchase",
            "DEBIT",
            "REF127",
            Category.inferredCategory("Shopping")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            date,
            amount,
            "Store Purchase",
            "Store",
            "Purchase",
            "DEBIT",
            "REF128",
            Category.inferredCategory("Shopping")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, date, date))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, date, date))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction2), // all duplicates matched
            List.of()                                    // none missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, date, date);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedCount()).isEqualTo(2);
        assertThat(result.getMissingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reconcile transactions within a specific date range")
    void shouldReconcileTransactionsWithinDateRange() {
        // Given
        AccountId accountId = AccountId.of("test-account-1");
        LocalDate fromDate = LocalDate.of(2024, 1, 10);
        LocalDate toDate = LocalDate.of(2024, 1, 20);
        
        // Transactions within range
        YnabTransaction ynabTransactionInRange1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase",
            Category.ynabCategory("shopping", "Shopping"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransactionInRange2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 18),
            Money.of(45.50),
            "Pharmacy",
            "Medicine",
            Category.ynabCategory("health", "Healthcare"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        BankTransaction bankTransactionInRange1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store Purchase",
            "Store",
            "Purchase",
            "DEBIT",
            "REF129",
            Category.inferredCategory("Shopping")
        );
        
        BankTransaction bankTransactionInRange2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 18),
            Money.of(45.50),
            "Pharmacy Purchase",
            "Pharmacy",
            "Medicine",
            "DEBIT",
            "REF130",
            Category.inferredCategory("Healthcare")
        );
        
        // Additional bank transaction without YNAB match
        BankTransaction bankTransactionMissing = new BankTransaction(
            TransactionId.of("bank-3"),
            accountId,
            LocalDate.of(2024, 1, 12),
            Money.of(25.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF131",
            Category.inferredCategory("Dining Out")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransactionInRange1, ynabTransactionInRange2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransactionInRange1, bankTransactionInRange2, bankTransactionMissing));
        
        mockReconciliationService(
            List.of(bankTransactionInRange1, bankTransactionInRange2), // 2 matched
            List.of(bankTransactionMissing)                            // 1 missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedCount()).isEqualTo(2);
        assertThat(result.getMissingCount()).isEqualTo(1);
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
        
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            targetAccountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store",
            "Purchase",
            Category.ynabCategory("shopping", "Shopping"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            targetAccountId,
            LocalDate.of(2024, 1, 18),
            Money.of(65.75),
            "Gas Station",
            "Fuel",
            Category.ynabCategory("transport", "Transportation"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            targetAccountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Store Purchase",
            "Store",
            "Purchase",
            "DEBIT",
            "REF132",
            Category.inferredCategory("Shopping")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            targetAccountId,
            LocalDate.of(2024, 1, 18),
            Money.of(65.75),
            "Gas Station Purchase",
            "Gas Station",
            "Fuel",
            "DEBIT",
            "REF133",
            Category.inferredCategory("Transportation")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(targetAccountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(targetAccountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction2), // all matched
            List.of()                                    // none missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(targetAccountId, fromDate, toDate);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedCount()).isEqualTo(2);
        assertThat(result.getMissingCount()).isEqualTo(0);
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
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 22),
            Money.of(75.50),
            "Restaurant",
            "Lunch",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        // Bank transactions with slight date differences (should match with RANGE strategy)
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 17), // 2 days later, should match
            Money.of(100.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF134",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 20), // 2 days earlier, should match
            Money.of(75.50),
            "Restaurant Purchase",
            "Restaurant",
            "Lunch",
            "DEBIT",
            "REF135",
            Category.inferredCategory("Dining Out")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction2), // all matched with RANGE
            List.of()                                    // none missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.RANGE);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingCount()).isEqualTo(0);
        assertThat(result.getMatchedCount()).isEqualTo(2);
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
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 10),
            Money.of(85.75),
            "Bookstore",
            "Books",
            Category.ynabCategory("education", "Education"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        // Bank transactions beyond the 3-day range (should NOT match with RANGE strategy)
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 20), // 5 days later, should NOT match
            Money.of(100.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF136",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 5), // 5 days earlier, should NOT match
            Money.of(85.75),
            "Bookstore Purchase",
            "Bookstore",
            "Books",
            "DEBIT",
            "REF137",
            Category.inferredCategory("Education")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(),                                   // none matched beyond range
            List.of(bankTransaction1, bankTransaction2)  // all missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.RANGE);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingCount()).isEqualTo(2);
        assertThat(result.getMatchedCount()).isEqualTo(0);
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
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(100.00),
            "Coffee Shop",
            "Morning coffee",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 25),
            Money.of(60.25),
            "Grocery Store",
            "Groceries",
            Category.ynabCategory("groceries", "Groceries"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        // Bank transactions with different dates (should NOT match with STRICT strategy)
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 16), // 1 day later, should NOT match with STRICT
            Money.of(100.00),
            "Coffee Shop Purchase",
            "Coffee Shop",
            "Morning coffee",
            "DEBIT",
            "REF138",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 24), // 1 day earlier, should NOT match with STRICT
            Money.of(60.25),
            "Grocery Store Purchase",
            "Grocery Store",
            "Groceries",
            "DEBIT",
            "REF139",
            Category.inferredCategory("Groceries")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2));
        
        mockReconciliationService(
            List.of(),                                   // none matched with STRICT different dates
            List.of(bankTransaction1, bankTransaction2)  // all missing
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMissingCount()).isEqualTo(2);
        assertThat(result.getMatchedCount()).isEqualTo(0);
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
        YnabTransaction ynabTransaction1 = new YnabTransaction(
            TransactionId.of("ynab-1"),
            accountId,
            LocalDate.of(2024, 1, 5),
            Money.of(150.00),
            "Grocery Store",
            "Weekly groceries",
            Category.ynabCategory("groceries", "Groceries"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction2 = new YnabTransaction(
            TransactionId.of("ynab-2"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(80.50),
            "Restaurant",
            "Lunch meeting",
            Category.ynabCategory("dining", "Dining Out"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        YnabTransaction ynabTransaction3 = new YnabTransaction(
            TransactionId.of("ynab-3"),
            accountId,
            LocalDate.of(2024, 1, 25),
            Money.of(45.75),
            "Bookstore",
            "Technical books",
            Category.ynabCategory("education", "Education"),
            ClearedStatus.CLEARED,
            true,
            null
        );
        
        // Bank transactions (4 total)
        BankTransaction bankTransaction1 = new BankTransaction(
            TransactionId.of("bank-1"),
            accountId,
            LocalDate.of(2024, 1, 5),
            Money.of(150.00),
            "Grocery Store XYZ",  // Matches ynab-1
            "Grocery Store XYZ",
            "Groceries",
            "DEBIT",
            "REF140",
            Category.inferredCategory("Groceries")
        );
        
        BankTransaction bankTransaction2 = new BankTransaction(
            TransactionId.of("bank-2"),
            accountId,
            LocalDate.of(2024, 1, 10),
            Money.of(35.25),
            "Gas Station Purchase",        // No YNAB match
            "Gas Station",
            "Fuel",
            "DEBIT",
            "REF141",
            Category.inferredCategory("Transportation")
        );
        
        BankTransaction bankTransaction3 = new BankTransaction(
            TransactionId.of("bank-3"),
            accountId,
            LocalDate.of(2024, 1, 15),
            Money.of(80.50),
            "Restaurant ABC Purchase",     // Matches ynab-2
            "Restaurant ABC",
            "Business lunch",
            "DEBIT",
            "REF142",
            Category.inferredCategory("Dining Out")
        );
        
        BankTransaction bankTransaction4 = new BankTransaction(
            TransactionId.of("bank-4"),
            accountId,
            LocalDate.of(2024, 1, 28),
            Money.of(22.99),
            "Coffee Shop Purchase",        // No YNAB match
            "Coffee Shop",
            "Coffee and pastry",
            "DEBIT",
            "REF143",
            Category.inferredCategory("Dining Out")
        );
        
        when(ynabTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(ynabTransaction1, ynabTransaction2, ynabTransaction3));
            
        when(bankTransactionRepository.findByAccountIdAndDateRange(accountId, fromDate, toDate))
            .thenReturn(List.of(bankTransaction1, bankTransaction2, bankTransaction3, bankTransaction4));
        
        mockReconciliationService(
            List.of(bankTransaction1, bankTransaction3),        // bank-1 & bank-3 matched
            List.of(bankTransaction2, bankTransaction4)         // bank-2 & bank-4 not in YNAB
        );
        
        ReconciliationRequest request = ReconciliationRequest.of(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
        
        // When
        ReconciliationResult result = reconcileTransactions.reconcile(request);
        
        // Then
        assertThat(result.getMatchedCount()).isEqualTo(2); // bank-1 & bank-3 matched
        assertThat(result.getMissingCount()).isEqualTo(2); // bank-2 & bank-4 not in YNAB
        assertThat(result.isFullyReconciled()).isFalse();
        assertThat(result.missingFromYnab()).hasSize(2);
        
        // Verify specific missing transactions
        var missingIds = result.missingFromYnab().stream()
            .map(BankTransaction::id)
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