package co.personal.ynabsyncher.service;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.matcher.TransactionMatcher;
import co.personal.ynabsyncher.model.matcher.TransactionMatcherFactory;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.model.reconciliation.TransactionMatchResult;
import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TransactionReconciliationService.
 * Tests both functional correctness and performance characteristics of the optimized matching algorithm.
 */
@DisplayName("TransactionReconciliationService")
class TransactionReconciliationServiceTest {

    private TransactionReconciliationService service;
    private AccountId testAccountId;

    @BeforeEach
    void setUp() {
        service = new TransactionReconciliationService();
        testAccountId = AccountId.of("test-account-123");
    }

    @Nested
    @DisplayName("Basic Functionality")
    class BasicFunctionality {

        @Test
        @DisplayName("should handle empty bank transactions")
        void shouldHandleEmptyBankTransactions() {
            List<BankTransaction> bankTransactions = List.of();
            List<YnabTransaction> ynabTransactions = List.of(createYnabTransaction("1", LocalDate.now(), 100));
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            assertEquals(0, result.getMatchedCount());
            assertEquals(0, result.getMissingFromYnabCount());
            assertTrue(result.matchedTransactions().isEmpty());
            assertTrue(result.missingFromYnab().isEmpty());
        }

        @Test
        @DisplayName("should handle empty YNAB transactions")
        void shouldHandleEmptyYnabTransactions() {
            List<BankTransaction> bankTransactions = List.of(createBankTransaction("1", LocalDate.now(), 100));
            List<YnabTransaction> ynabTransactions = List.of();
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            assertEquals(0, result.getMatchedCount());
            assertEquals(1, result.getMissingFromYnabCount());
            assertTrue(result.matchedTransactions().isEmpty());
            assertEquals(1, result.missingFromYnab().size());
        }

        @Test
        @DisplayName("should find exact matches with strict strategy")
        void shouldFindExactMatchesWithStrictStrategy() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("1", date, 100),
                createBankTransaction("2", date, 200)
            );
            List<YnabTransaction> ynabTransactions = List.of(
                createYnabTransaction("1", date, 100),
                createYnabTransaction("2", date, 300) // Different amount, shouldn't match
            );
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            assertEquals(1, result.getMatchedCount());
            assertEquals(1, result.getMissingFromYnabCount());
        }

        @ParameterizedTest
        @EnumSource(ReconciliationStrategy.class)
        @DisplayName("should work with all reconciliation strategies")
        void shouldWorkWithAllReconciliationStrategies(ReconciliationStrategy strategy) {
            LocalDate date = LocalDate.of(2024, 1, 15);
            List<BankTransaction> bankTransactions = List.of(createBankTransaction("1", date, 100));
            List<YnabTransaction> ynabTransactions = List.of(createYnabTransaction("1", date, 100));
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(strategy);

            assertDoesNotThrow(() -> {
                TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);
                assertNotNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Date Ordering and Performance")
    class DateOrderingAndPerformance {

        @Test
        @DisplayName("should process transactions in chronological order regardless of input order")
        void shouldProcessTransactionsInChronologicalOrder() {
            // Create bank transactions in reverse chronological order
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("3", LocalDate.of(2024, 1, 20), 300),
                createBankTransaction("1", LocalDate.of(2024, 1, 10), 100),
                createBankTransaction("2", LocalDate.of(2024, 1, 15), 200)
            );
            
            // Create matching YNAB transactions in different order
            List<YnabTransaction> ynabTransactions = List.of(
                createYnabTransaction("2", LocalDate.of(2024, 1, 15), 200),
                createYnabTransaction("3", LocalDate.of(2024, 1, 20), 300),
                createYnabTransaction("1", LocalDate.of(2024, 1, 10), 100)
            );

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            // All should match regardless of input order
            assertEquals(3, result.getMatchedCount());
            assertEquals(0, result.getMissingFromYnabCount());
            
            // Verify that all transactions were matched
            assertEquals(3, result.matchedTransactions().size());
            assertTrue(result.missingFromYnab().isEmpty());
        }

        @Test
        @DisplayName("should handle large datasets efficiently")
        void shouldHandleLargeDatasetsEfficiently() {
            int transactionCount = 1000;
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            
            // Create large datasets with matching transactions
            List<BankTransaction> bankTransactions = IntStream.range(0, transactionCount)
                .mapToObj(i -> createBankTransaction(
                    String.valueOf(i), 
                    startDate.plusDays(i % 365), 
                    100 + i
                ))
                .toList();
                
            List<YnabTransaction> ynabTransactions = IntStream.range(0, transactionCount)
                .mapToObj(i -> createYnabTransaction(
                    String.valueOf(i), 
                    startDate.plusDays(i % 365), 
                    100 + i
                ))
                .toList();

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            // Measure execution time
            long startTime = System.nanoTime();
            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            // Verify correctness
            assertEquals(transactionCount, result.getMatchedCount());
            assertEquals(0, result.getMissingFromYnabCount());
            
            // Performance should be reasonable (less than 1 second for 1000 transactions)
            assertTrue(durationMs < 1000, 
                "Processing " + transactionCount + " transactions took " + durationMs + "ms, expected < 1000ms");
        }

        @Test
        @DisplayName("should handle worst-case scenario with scattered dates")
        void shouldHandleWorstCaseScenarioWithScatteredDates() {
            LocalDate baseDate = LocalDate.of(2024, 1, 1);
            
            // Create transactions with scattered dates to stress-test binary search
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("1", baseDate.plusDays(100), 100),
                createBankTransaction("2", baseDate.plusDays(50), 200),
                createBankTransaction("3", baseDate.plusDays(200), 300),
                createBankTransaction("4", baseDate.plusDays(10), 400),
                createBankTransaction("5", baseDate.plusDays(150), 500)
            );
            
            // Create a large YNAB dataset with every day covered
            List<YnabTransaction> ynabTransactions = IntStream.range(0, 365)
                .mapToObj(i -> createYnabTransaction(
                    "ynab-" + i,
                    baseDate.plusDays(i),
                    1000 + i
                ))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            // Add matching transactions scattered throughout
            ynabTransactions.add(createYnabTransaction("match-1", baseDate.plusDays(100), 100));
            ynabTransactions.add(createYnabTransaction("match-2", baseDate.plusDays(50), 200));
            ynabTransactions.add(createYnabTransaction("match-3", baseDate.plusDays(200), 300));

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            // Should find the 3 matches efficiently
            assertEquals(3, result.getMatchedCount());
            assertEquals(2, result.getMissingFromYnabCount());
        }
    }

    @Nested
    @DisplayName("Range Matching Strategy")
    class RangeMatchingStrategy {

        @Test
        @DisplayName("should find matches within date range window")
        void shouldFindMatchesWithinDateRangeWindow() {
            LocalDate bankDate = LocalDate.of(2024, 1, 15);
            LocalDate ynabDateWithinRange = LocalDate.of(2024, 1, 17); // 2 days later, within 3-day window
            
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("1", bankDate, 100)
            );
            List<YnabTransaction> ynabTransactions = List.of(
                createYnabTransaction("1", ynabDateWithinRange, 100)
            );

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.RANGE);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            assertEquals(1, result.getMatchedCount());
            assertEquals(0, result.getMissingFromYnabCount());
        }

        @Test
        @DisplayName("should not find matches outside date range window")
        void shouldNotFindMatchesOutsideDateRangeWindow() {
            LocalDate bankDate = LocalDate.of(2024, 1, 15);
            LocalDate ynabDateOutsideRange = LocalDate.of(2024, 1, 25); // 10 days later, outside 3-day window
            
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("1", bankDate, 100)
            );
            List<YnabTransaction> ynabTransactions = List.of(
                createYnabTransaction("1", ynabDateOutsideRange, 100)
            );

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.RANGE);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            assertEquals(0, result.getMatchedCount());
            assertEquals(1, result.getMissingFromYnabCount());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("should throw NPE for null bank transactions")
        void shouldThrowNpeForNullBankTransactions() {
            List<YnabTransaction> ynabTransactions = List.of();
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            assertThrows(NullPointerException.class, () -> {
                service.reconcileTransactions(null, ynabTransactions, matcher);
            });
        }

        @Test
        @DisplayName("should throw NPE for null YNAB transactions")
        void shouldThrowNpeForNullYnabTransactions() {
            List<BankTransaction> bankTransactions = List.of();
            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            assertThrows(NullPointerException.class, () -> {
                service.reconcileTransactions(bankTransactions, null, matcher);
            });
        }

        @Test
        @DisplayName("should throw NPE for null matcher")
        void shouldThrowNpeForNullMatcher() {
            List<BankTransaction> bankTransactions = List.of();
            List<YnabTransaction> ynabTransactions = List.of();

            assertThrows(NullPointerException.class, () -> {
                service.reconcileTransactions(bankTransactions, ynabTransactions, null);
            });
        }

        @Test
        @DisplayName("should handle duplicate transactions correctly")
        void shouldHandleDuplicateTransactionsCorrectly() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            
            // Create duplicate bank transactions
            List<BankTransaction> bankTransactions = List.of(
                createBankTransaction("1", date, 100),
                createBankTransaction("2", date, 100) // Same amount and date
            );
            
            // Only one matching YNAB transaction
            List<YnabTransaction> ynabTransactions = List.of(
                createYnabTransaction("1", date, 100)
            );

            TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            TransactionMatchResult result = service.reconcileTransactions(bankTransactions, ynabTransactions, matcher);

            // Only one should match (first match wins)
            assertEquals(1, result.getMatchedCount());
            assertEquals(1, result.getMissingFromYnabCount());
        }
    }

    // Helper methods for creating test data
    private BankTransaction createBankTransaction(String id, LocalDate date, int amountCents) {
        return BankTransaction.withUnknownCategory(
            TransactionId.of(id),
            testAccountId,
            date,
            Money.ofMilliunits(amountCents * 10), // Convert cents to milliunits (10 milliunits = 1 cent)
            "Test bank transaction " + id,
            "Test merchant " + id,
            "Test memo " + id,
            "purchase",
            "ref-" + id
        );
    }

    private YnabTransaction createYnabTransaction(String id, LocalDate date, int amountCents) {
        return new YnabTransaction(
            TransactionId.of(id),
            testAccountId,
            date,
            Money.ofMilliunits(amountCents * 10), // Convert cents to milliunits (10 milliunits = 1 cent)
            "Test payee " + id,
            "Test memo " + id,
            co.personal.ynabsyncher.model.Category.unknown(),
            ClearedStatus.CLEARED,
            true,
            null
        );
    }
}