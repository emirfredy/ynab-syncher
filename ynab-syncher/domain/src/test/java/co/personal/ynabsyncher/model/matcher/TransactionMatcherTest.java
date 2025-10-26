package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.bank.BankTransactionAdapter;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.model.ynab.YnabTransactionAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static co.personal.ynabsyncher.model.BankTransactionTestBuilder.aBankTransaction;
import static co.personal.ynabsyncher.model.YnabTransactionTestBuilder.aYnabTransaction;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Matcher Tests")
class TransactionMatcherTest {

    private final AccountId testAccountId = AccountId.of("test-account-123");
    private final AccountId otherAccountId = AccountId.of("other-account-456");
    private final Money standardAmount = Money.of(100.00);
    private final LocalDate baseDate = LocalDate.of(2024, 10, 15);

    @Nested
    @DisplayName("Strict Transaction Matcher")
    class StrictTransactionMatcherTest {

        private final TransactionMatcher strictMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

        @Test
        @DisplayName("Should match transactions with identical core fields")
        void shouldMatchIdenticalTransactions() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate, standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should not match transactions with different amounts")
        void shouldNotMatchDifferentAmounts() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, Money.of(100.00));
            var bankTransaction = createBankTransaction(testAccountId, baseDate, Money.of(200.00));

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should not match transactions with different accounts")
        void shouldNotMatchDifferentAccounts() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(otherAccountId, baseDate, standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should not match transactions with different dates")
        void shouldNotMatchDifferentDates() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate.plusDays(1), standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should not match transactions from same source")
        void shouldNotMatchSameSource() {
            // Given - Two YNAB transactions (same source)
            var ynabTransaction1 = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var ynabTransaction2 = createYnabTransaction(testAccountId, baseDate, standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction1),
                new YnabTransactionAdapter(ynabTransaction2)
            )).isFalse();
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.01, 50.00, 999.99, 1000.00, -25.50})
        @DisplayName("Should match transactions with various amount values")
        void shouldMatchVariousAmounts(double amountValue) {
            // Given
            var amount = Money.of(amountValue);
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, amount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate, amount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should handle null memo fields gracefully")
        void shouldHandleNullMemos() {
            // Given
            var ynabTransaction = aYnabTransaction()
                .withId("ynab-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withPayeeName("Test Payee")
                .withMemo(null)
                .build();

            var bankTransaction = aBankTransaction()
                .withId("bank-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withDescription("Test Description")
                .withMemo(null)
                .build();

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }
    }

    @Nested
    @DisplayName("Range Transaction Matcher")
    class RangeTransactionMatcherTest {

        private final TransactionMatcher rangeMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.RANGE);

        @Test
        @DisplayName("Should match transactions with same date")
        void shouldMatchSameDate() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate, standardAmount);

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("Should match transactions within acceptable date range")
        void shouldMatchWithinDateRange(int daysDifference) {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate.plusDays(daysDifference), standardAmount);

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("Should match transactions within acceptable date range (negative days)")
        void shouldMatchWithinDateRangeNegative(int daysDifference) {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate.minusDays(daysDifference), standardAmount);

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 5, 7, 10, 30})
        @DisplayName("Should not match transactions outside acceptable date range")
        void shouldNotMatchOutsideDateRange(int daysDifference) {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate.plusDays(daysDifference), standardAmount);

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should not match transactions with different amounts even within date range")
        void shouldNotMatchDifferentAmountsInRange() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, Money.of(100.00));
            var bankTransaction = createBankTransaction(testAccountId, baseDate.plusDays(2), Money.of(200.00));

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should not match transactions with different accounts even within date range")
        void shouldNotMatchDifferentAccountsInRange() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, standardAmount);
            var bankTransaction = createBankTransaction(otherAccountId, baseDate.plusDays(2), standardAmount);

            // When & Then
            assertThat(rangeMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }
    }

    @Nested
    @DisplayName("Category-Aware Matching Enhancement")
    class CategoryAwareMatchingTest {

        private final TransactionMatcher strictMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

        @Test
        @DisplayName("Should match transactions with similar categories")
        void shouldMatchSimilarCategories() {
            // Given
            var groceriesCategory = Category.ynabCategory("cat-1", "Groceries");
            var foodCategory = Category.inferredCategory("Food & Dining");

            var ynabTransaction = aYnabTransaction()
                .withId("ynab-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withPayeeName("Whole Foods")
                .withCategory(groceriesCategory)
                .build();

            var bankTransaction = aBankTransaction()
                .withId("bank-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withDescription("WHOLE FOODS MARKET")
                .withInferredCategory(foodCategory)
                .build();

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should match transactions when one category is unknown")
        void shouldMatchWhenCategoryUnknown() {
            // Given
            var knownCategory = Category.ynabCategory("cat-1", "Dining");
            var unknownCategory = Category.unknown();

            var ynabTransaction = aYnabTransaction()
                .withId("ynab-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withPayeeName("Restaurant")
                .withCategory(knownCategory)
                .build();

            var bankTransaction = aBankTransaction()
                .withId("bank-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withDescription("RESTAURANT XYZ")
                .withInferredCategory(unknownCategory)
                .build();

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should handle matching with different but compatible categories")
        void shouldMatchCompatibleCategories() {
            // Given
            var ynabCategory = Category.ynabCategory("cat-1", "Entertainment");
            var bankCategory = Category.inferredCategory("Recreation");

            var ynabTransaction = aYnabTransaction()
                .withId("ynab-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withPayeeName("Movie Theater")
                .withCategory(ynabCategory)
                .build();

            var bankTransaction = aBankTransaction()
                .withId("bank-1")
                .withAccountId(testAccountId)
                .withDate(baseDate)
                .withAmount(standardAmount)
                .withDescription("CINEMA COMPLEX")
                .withInferredCategory(bankCategory)
                .build();

            // When & Then - Current implementation is conservative and allows different categories
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesTest {

        private final TransactionMatcher strictMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

        @Test
        @DisplayName("Should handle zero amount transactions")
        void shouldHandleZeroAmounts() {
            // Given
            var zeroAmount = Money.of(0.00);
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, zeroAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate, zeroAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should handle negative amount transactions")
        void shouldHandleNegativeAmounts() {
            // Given
            var negativeAmount = Money.of(-50.75);
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, negativeAmount);
            var bankTransaction = createBankTransaction(testAccountId, baseDate, negativeAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should handle very small amount differences")
        void shouldNotMatchVerySmallAmountDifferences() {
            // Given
            var ynabTransaction = createYnabTransaction(testAccountId, baseDate, Money.of(100.00));
            var bankTransaction = createBankTransaction(testAccountId, baseDate, Money.of(100.01));

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isFalse();
        }

        @Test
        @DisplayName("Should handle transactions on leap year dates")
        void shouldHandleLeapYearDates() {
            // Given
            var leapYearDate = LocalDate.of(2024, 2, 29); // 2024 is a leap year
            var ynabTransaction = createYnabTransaction(testAccountId, leapYearDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, leapYearDate, standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }

        @Test
        @DisplayName("Should handle transactions at year boundaries")
        void shouldHandleYearBoundaryDates() {
            // Given
            var yearEndDate = LocalDate.of(2023, 12, 31);
            var ynabTransaction = createYnabTransaction(testAccountId, yearEndDate, standardAmount);
            var bankTransaction = createBankTransaction(testAccountId, yearEndDate, standardAmount);

            // When & Then
            assertThat(strictMatcher.matches(
                new YnabTransactionAdapter(ynabTransaction),
                new BankTransactionAdapter(bankTransaction)
            )).isTrue();
        }
    }

    @Nested
    @DisplayName("Factory and Strategy Tests")
    class FactoryAndStrategyTest {

        @ParameterizedTest
        @EnumSource(ReconciliationStrategy.class)
        @DisplayName("Should create matchers for all reconciliation strategies")
        void shouldCreateMatchersForAllStrategies(ReconciliationStrategy strategy) {
            // When
            var matcher = TransactionMatcherFactory.createMatcher(strategy);

            // Then
            assertThat(matcher).isNotNull();
            assertThat(matcher.getStrategy()).isEqualTo(strategy);
        }

        @Test
        @DisplayName("Should return correct strategy from strict matcher")
        void shouldReturnCorrectStrategyFromStrictMatcher() {
            // Given
            var matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);

            // When & Then
            assertThat(matcher.getStrategy()).isEqualTo(ReconciliationStrategy.STRICT);
        }

        @Test
        @DisplayName("Should return correct strategy from range matcher")
        void shouldReturnCorrectStrategyFromRangeMatcher() {
            // Given
            var matcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.RANGE);

            // When & Then
            assertThat(matcher.getStrategy()).isEqualTo(ReconciliationStrategy.RANGE);
        }
    }

    // Helper methods for creating test data
    private YnabTransaction createYnabTransaction(AccountId accountId, LocalDate date, Money amount) {
        return aYnabTransaction()
            .withId("ynab-" + System.nanoTime())
            .withAccountId(accountId)
            .withDate(date)
            .withAmount(amount)
            .withPayeeName("Test Payee")
            .withMemo("Test memo")
            .withCategory(Category.ynabCategory("cat-123", "Test Category"))
            .withClearedStatus(ClearedStatus.CLEARED)
            .build();
    }

    private BankTransaction createBankTransaction(AccountId accountId, LocalDate date, Money amount) {
        return aBankTransaction()
            .withId("bank-" + System.nanoTime())
            .withAccountId(accountId)
            .withDate(date)
            .withAmount(amount)
            .withDescription("Test bank transaction")
            .withMemo("Test bank memo")
            .withInferredCategory(Category.inferredCategory("Test Category"))
            .build();
    }
}
