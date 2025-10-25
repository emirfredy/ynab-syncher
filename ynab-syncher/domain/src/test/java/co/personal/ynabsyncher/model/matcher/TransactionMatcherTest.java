package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Matcher Tests")
class TransactionMatcherTest {

    private final AccountId accountId = AccountId.of("test-account");
    private final Money amount = Money.of(100.00);

    @Test
    @DisplayName("Strict matcher should match transactions with same date, amount, and account")
    void strictMatcherShouldMatchExactTransactions() {
        // Given
        TransactionMatcher matcher = new StrictTransactionMatcher();
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, date, amount, "Payee1", "Memo1"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, date, amount, "Payee2", "Memo2"
        );
        
        // When & Then
        assertThat(matcher.matches(transaction1, transaction2)).isTrue();
    }

    @Test
    @DisplayName("Strict matcher should not match transactions with different dates")
    void strictMatcherShouldNotMatchDifferentDates() {
        // Given
        TransactionMatcher matcher = new StrictTransactionMatcher();
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, LocalDate.of(2024, 1, 15), amount, "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, LocalDate.of(2024, 1, 16), amount, "Payee", "Memo"
        );
        
        // When & Then
        assertThat(matcher.matches(transaction1, transaction2)).isFalse();
    }

    @Test
    @DisplayName("Range matcher should match transactions within 3 days")
    void rangeMatcherShouldMatchWithinThreeDays() {
        // Given
        TransactionMatcher matcher = new RangeTransactionMatcher();
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, LocalDate.of(2024, 1, 15), amount, "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, LocalDate.of(2024, 1, 18), amount, "Payee", "Memo"
        );
        
        // When & Then (3 days difference should match)
        assertThat(matcher.matches(transaction1, transaction2)).isTrue();
    }

    @Test
    @DisplayName("Range matcher should not match transactions more than 3 days apart")
    void rangeMatcherShouldNotMatchBeyondThreeDays() {
        // Given
        TransactionMatcher matcher = new RangeTransactionMatcher();
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, LocalDate.of(2024, 1, 15), amount, "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, LocalDate.of(2024, 1, 19), amount, "Payee", "Memo"
        );
        
        // When & Then (4 days difference should not match)
        assertThat(matcher.matches(transaction1, transaction2)).isFalse();
    }

    @Test
    @DisplayName("Range matcher should match transactions exactly 3 days apart")
    void rangeMatcherShouldMatchExactlyThreeDaysApart() {
        // Given
        TransactionMatcher matcher = new RangeTransactionMatcher();
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, LocalDate.of(2024, 1, 15), amount, "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, LocalDate.of(2024, 1, 12), amount, "Payee", "Memo"
        );
        
        // When & Then (exactly 3 days difference should match)
        assertThat(matcher.matches(transaction1, transaction2)).isTrue();
    }

    @Test
    @DisplayName("Both matchers should not match transactions with different amounts")
    void matchersShouldNotMatchDifferentAmounts() {
        // Given
        TransactionMatcher strictMatcher = new StrictTransactionMatcher();
        TransactionMatcher rangeMatcher = new RangeTransactionMatcher();
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), accountId, date, Money.of(100.00), "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), accountId, date, Money.of(200.00), "Payee", "Memo"
        );
        
        // When & Then
        assertThat(strictMatcher.matches(transaction1, transaction2)).isFalse();
        assertThat(rangeMatcher.matches(transaction1, transaction2)).isFalse();
    }

    @Test
    @DisplayName("Both matchers should not match transactions with different accounts")
    void matchersShouldNotMatchDifferentAccounts() {
        // Given
        TransactionMatcher strictMatcher = new StrictTransactionMatcher();
        TransactionMatcher rangeMatcher = new RangeTransactionMatcher();
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        Transaction transaction1 = Transaction.ynabTransaction(
            TransactionId.of("1"), AccountId.of("account1"), date, amount, "Payee", "Memo"
        );
        
        Transaction transaction2 = Transaction.bankTransaction(
            TransactionId.of("2"), AccountId.of("account2"), date, amount, "Payee", "Memo"
        );
        
        // When & Then
        assertThat(strictMatcher.matches(transaction1, transaction2)).isFalse();
        assertThat(rangeMatcher.matches(transaction1, transaction2)).isFalse();
    }

    @Test
    @DisplayName("Factory should create correct matcher for each strategy")
    void factoryShouldCreateCorrectMatchers() {
        // When
        TransactionMatcher strictMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.STRICT);
        TransactionMatcher rangeMatcher = TransactionMatcherFactory.createMatcher(ReconciliationStrategy.RANGE);
        
        // Then
        assertThat(strictMatcher).isInstanceOf(StrictTransactionMatcher.class);
        assertThat(strictMatcher.getStrategy()).isEqualTo(ReconciliationStrategy.STRICT);
        
        assertThat(rangeMatcher).isInstanceOf(RangeTransactionMatcher.class);
        assertThat(rangeMatcher.getStrategy()).isEqualTo(ReconciliationStrategy.RANGE);
    }
}