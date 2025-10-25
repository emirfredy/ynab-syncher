package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ReconciliationSummaryTest {

    private final AccountId accountId = AccountId.of("test-account");
    private final LocalDate reconciliationDate = LocalDate.of(2024, 10, 24);
    private final LocalDate fromDate = LocalDate.of(2024, 10, 1);
    private final LocalDate toDate = LocalDate.of(2024, 10, 23);
    private final ReconciliationStrategy strategy = ReconciliationStrategy.STRICT;

    @Nested
    class Construction {
        
        @Test
        void shouldCreateValidSummary() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            assertThat(summary.accountId()).isEqualTo(accountId);
            assertThat(summary.reconciliationDate()).isEqualTo(reconciliationDate);
            assertThat(summary.fromDate()).isEqualTo(fromDate);
            assertThat(summary.toDate()).isEqualTo(toDate);
            assertThat(summary.strategy()).isEqualTo(strategy);
            assertThat(summary.totalBankTransactions()).isEqualTo(10);
            assertThat(summary.totalYnabTransactions()).isEqualTo(8);
            assertThat(summary.matchedTransactions()).isEqualTo(7);
            assertThat(summary.missingFromYnab()).isEqualTo(3);
        }
        
        @Test
        void shouldCreateSummaryWithZeroValues() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                0, 0, 0, 0
            );
            
            assertThat(summary.totalBankTransactions()).isEqualTo(0);
            assertThat(summary.totalYnabTransactions()).isEqualTo(0);
            assertThat(summary.matchedTransactions()).isEqualTo(0);
            assertThat(summary.missingFromYnab()).isEqualTo(0);
        }
        
        @Test
        void shouldThrowWhenAccountIdIsNull() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                null, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Account ID cannot be null");
        }
        
        @Test
        void shouldThrowWhenReconciliationDateIsNull() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, null, fromDate, toDate, strategy,
                10, 8, 7, 3
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Reconciliation date cannot be null");
        }
        
        @Test
        void shouldThrowWhenFromDateIsNull() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, null, toDate, strategy,
                10, 8, 7, 3
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("From date cannot be null");
        }
        
        @Test
        void shouldThrowWhenToDateIsNull() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, null, strategy,
                10, 8, 7, 3
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("To date cannot be null");
        }
        
        @Test
        void shouldThrowWhenStrategyIsNull() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, null,
                10, 8, 7, 3
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Reconciliation strategy cannot be null");
        }
        
        @Test
        void shouldThrowWhenTotalBankTransactionsIsNegative() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                -1, 8, 7, 3
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total bank transactions cannot be negative");
        }
        
        @Test
        void shouldThrowWhenTotalYnabTransactionsIsNegative() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, -1, 7, 3
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total YNAB transactions cannot be negative");
        }
        
        @Test
        void shouldThrowWhenMatchedTransactionsIsNegative() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, -1, 3
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Matched transactions cannot be negative");
        }
        
        @Test
        void shouldThrowWhenMissingFromYnabIsNegative() {
            assertThatThrownBy(() -> new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, -1
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing from YNAB cannot be negative");
        }
    }

    @Nested
    class ReconciliationPercentage {
        
        @Test
        void shouldCalculatePercentageWhenAllMatched() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 10, 0
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(100.0);
        }
        
        @Test
        void shouldCalculatePercentageWhenPartiallyMatched() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(70.0);
        }
        
        @Test
        void shouldCalculatePercentageWhenNoneMatched() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 0, 10
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(0.0);
        }
        
        @Test
        void shouldReturn100PercentWhenNoBankTransactions() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                0, 5, 0, 0
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(100.0);
        }
        
        @Test
        void shouldCalculatePercentageWithPrecision() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                3, 8, 1, 2
            );
            
            assertThat(summary.getReconciliationPercentage())
                .isCloseTo(33.333333333333336, within(0.000001));
        }
        
        @Test
        void shouldCalculatePercentageForSingleTransaction() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                1, 1, 1, 0
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(100.0);
        }
    }

    @Nested
    class CompletionStatus {
        
        @Test
        void shouldBeCompleteWhenNoMissingTransactions() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 10, 0
            );
            
            assertThat(summary.isComplete()).isTrue();
        }
        
        @Test
        void shouldNotBeCompleteWhenHasMissingTransactions() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            assertThat(summary.isComplete()).isFalse();
        }
        
        @Test
        void shouldBeCompleteWhenNoBankTransactions() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                0, 5, 0, 0
            );
            
            assertThat(summary.isComplete()).isTrue();
        }
        
        @Test
        void shouldNotBeCompleteWhenOnlyOneMissing() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 9, 1
            );
            
            assertThat(summary.isComplete()).isFalse();
        }
    }

    @Nested
    class Equality {
        
        @Test
        void shouldBeEqualWhenAllFieldsMatch() {
            ReconciliationSummary summary1 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            ReconciliationSummary summary2 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            assertThat(summary1).isEqualTo(summary2);
            assertThat(summary1.hashCode()).isEqualTo(summary2.hashCode());
        }
        
        @Test
        void shouldNotBeEqualWhenAccountIdDiffers() {
            AccountId differentAccountId = AccountId.of("different-account");
            ReconciliationSummary summary1 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            ReconciliationSummary summary2 = new ReconciliationSummary(
                differentAccountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            assertThat(summary1).isNotEqualTo(summary2);
        }
        
        @Test
        void shouldNotBeEqualWhenCountsDiffer() {
            ReconciliationSummary summary1 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            ReconciliationSummary summary2 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                9, 8, 7, 3  // Different totalBankTransactions
            );
            
            assertThat(summary1).isNotEqualTo(summary2);
        }
        
        @Test
        void shouldNotBeEqualWhenStrategyDiffers() {
            ReconciliationSummary summary1 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, ReconciliationStrategy.STRICT,
                10, 8, 7, 3
            );
            ReconciliationSummary summary2 = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, ReconciliationStrategy.RANGE,
                10, 8, 7, 3
            );
            
            assertThat(summary1).isNotEqualTo(summary2);
        }
    }

    @Nested
    class ToString {
        
        @Test
        void shouldHaveDescriptiveToString() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                10, 8, 7, 3
            );
            
            String result = summary.toString();
            
            assertThat(result).contains("ReconciliationSummary");
            assertThat(result).contains(accountId.toString());
            assertThat(result).contains("10");
            assertThat(result).contains("8");
            assertThat(result).contains("7");
            assertThat(result).contains("3");
        }
    }

    @Nested
    class EdgeCases {
        
        @Test
        void shouldHandleLargeNumbers() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(100.0);
            assertThat(summary.isComplete()).isTrue();
        }
        
        @Test
        void shouldHandleMoreMatchedThanBank() {
            // This could happen in edge cases where YNAB has more detailed records
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                5, 10, 5, 0
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(100.0);
            assertThat(summary.isComplete()).isTrue();
        }
        
        @Test
        void shouldHandleVerySmallPercentages() {
            ReconciliationSummary summary = new ReconciliationSummary(
                accountId, reconciliationDate, fromDate, toDate, strategy,
                1000, 500, 1, 999
            );
            
            assertThat(summary.getReconciliationPercentage()).isEqualTo(0.1);
            assertThat(summary.isComplete()).isFalse();
        }
    }
}