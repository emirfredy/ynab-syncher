package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ReconciliationRequestTest {

    private final AccountId accountId = AccountId.of("test-account");
    private final LocalDate fromDate = LocalDate.of(2024, 1, 1);
    private final LocalDate toDate = LocalDate.of(2024, 1, 31);

    @Nested
    class Construction {
        
        @Test
        void shouldCreateValidRequest() {
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.fromDate()).isEqualTo(fromDate);
            assertThat(request.toDate()).isEqualTo(toDate);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.STRICT);
        }
        
        @Test
        void shouldThrowWhenAccountIdIsNull() {
            assertThatThrownBy(() -> new ReconciliationRequest(
                null, fromDate, toDate, ReconciliationStrategy.STRICT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Account ID cannot be null");
        }
        
        @Test
        void shouldThrowWhenFromDateIsNull() {
            assertThatThrownBy(() -> new ReconciliationRequest(
                accountId, null, toDate, ReconciliationStrategy.STRICT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("From date cannot be null");
        }
        
        @Test
        void shouldThrowWhenToDateIsNull() {
            assertThatThrownBy(() -> new ReconciliationRequest(
                accountId, fromDate, null, ReconciliationStrategy.STRICT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("To date cannot be null");
        }
        
        @Test
        void shouldThrowWhenStrategyIsNull() {
            assertThatThrownBy(() -> new ReconciliationRequest(
                accountId, fromDate, toDate, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Reconciliation strategy cannot be null");
        }
        
        @Test
        void shouldThrowWhenFromDateIsAfterToDate() {
            LocalDate afterToDate = toDate.plusDays(1);
            
            assertThatThrownBy(() -> new ReconciliationRequest(
                accountId, afterToDate, toDate, ReconciliationStrategy.STRICT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From date cannot be after to date");
        }
        
        @Test
        void shouldAllowSameFromAndToDate() {
            LocalDate sameDate = LocalDate.of(2024, 1, 15);
            
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, sameDate, sameDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.fromDate()).isEqualTo(sameDate);
            assertThat(request.toDate()).isEqualTo(sameDate);
        }
    }

    @Nested
    class FactoryMethods {
        
        @Test
        void shouldCreateWithFullParameters() {
            ReconciliationRequest request = ReconciliationRequest.of(
                accountId, fromDate, toDate, ReconciliationStrategy.RANGE
            );
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.fromDate()).isEqualTo(fromDate);
            assertThat(request.toDate()).isEqualTo(toDate);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.RANGE);
        }
        
        @Test
        void shouldCreateWithDefaultStrictStrategy() {
            ReconciliationRequest request = ReconciliationRequest.of(
                accountId, fromDate, toDate
            );
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.fromDate()).isEqualTo(fromDate);
            assertThat(request.toDate()).isEqualTo(toDate);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.STRICT);
        }
        
        @Test
        void shouldCreateForLast30DaysWithStrictStrategy() {
            ReconciliationRequest request = ReconciliationRequest.forLast30Days(accountId);
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.STRICT);
            assertThat(request.getDayCount()).isEqualTo(31); // 30 days + 1 for inclusive dates
        }
        
        @Test
        void shouldCreateForLast30DaysWithSpecifiedStrategy() {
            ReconciliationRequest request = ReconciliationRequest.forLast30Days(
                accountId, ReconciliationStrategy.RANGE
            );
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.RANGE);
            assertThat(request.getDayCount()).isEqualTo(31);
        }
        
        @Test
        void shouldCreateForCurrentMonthWithStrictStrategy() {
            ReconciliationRequest request = ReconciliationRequest.forCurrentMonth(accountId);
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.STRICT);
            assertThat(request.fromDate().getDayOfMonth()).isEqualTo(1);
            assertThat(request.fromDate().getMonth()).isEqualTo(LocalDate.now().getMonth());
            assertThat(request.fromDate().getYear()).isEqualTo(LocalDate.now().getYear());
        }
        
        @Test
        void shouldCreateForCurrentMonthWithSpecifiedStrategy() {
            ReconciliationRequest request = ReconciliationRequest.forCurrentMonth(
                accountId, ReconciliationStrategy.RANGE
            );
            
            assertThat(request.accountId()).isEqualTo(accountId);
            assertThat(request.strategy()).isEqualTo(ReconciliationStrategy.RANGE);
            assertThat(request.fromDate().getDayOfMonth()).isEqualTo(1);
        }
    }

    @Nested
    class DayCountCalculation {
        
        @Test
        void shouldCalculateDayCountForSingleDay() {
            LocalDate singleDate = LocalDate.of(2024, 1, 15);
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, singleDate, singleDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.getDayCount()).isEqualTo(1);
        }
        
        @Test
        void shouldCalculateDayCountForMultipleDays() {
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 10);
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, start, end, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.getDayCount()).isEqualTo(10); // 1st to 10th inclusive
        }
        
        @Test
        void shouldCalculateDayCountForFullMonth() {
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, start, end, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.getDayCount()).isEqualTo(31);
        }
        
        @Test
        void shouldCalculateDayCountAcrossMonths() {
            LocalDate start = LocalDate.of(2024, 1, 31);
            LocalDate end = LocalDate.of(2024, 2, 2);
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, start, end, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.getDayCount()).isEqualTo(3); // Jan 31, Feb 1, Feb 2
        }
        
        @Test
        void shouldCalculateDayCountForLeapYear() {
            LocalDate start = LocalDate.of(2024, 2, 28); // 2024 is a leap year
            LocalDate end = LocalDate.of(2024, 3, 1);
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, start, end, ReconciliationStrategy.STRICT
            );
            
            assertThat(request.getDayCount()).isEqualTo(3); // Feb 28, Feb 29, Mar 1
        }
    }

    @Nested
    class Equality {
        
        @Test
        void shouldBeEqualWhenAllFieldsMatch() {
            ReconciliationRequest request1 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            ReconciliationRequest request2 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }
        
        @Test
        void shouldNotBeEqualWhenAccountIdDiffers() {
            AccountId differentAccountId = AccountId.of("different-account");
            ReconciliationRequest request1 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            ReconciliationRequest request2 = new ReconciliationRequest(
                differentAccountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request1).isNotEqualTo(request2);
        }
        
        @Test
        void shouldNotBeEqualWhenStrategyDiffers() {
            ReconciliationRequest request1 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            ReconciliationRequest request2 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.RANGE
            );
            
            assertThat(request1).isNotEqualTo(request2);
        }
        
        @Test
        void shouldNotBeEqualWhenDatesD‌iffer() {
            LocalDate differentFromDate = fromDate.plusDays(1);
            ReconciliationRequest request1 = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            ReconciliationRequest request2 = new ReconciliationRequest(
                accountId, differentFromDate, toDate, ReconciliationStrategy.STRICT
            );
            
            assertThat(request1).isNotEqualTo(request2);
        }
    }

    @Nested
    class ToString {
        
        @Test
        void shouldHaveDescriptiveToString() {
            ReconciliationRequest request = new ReconciliationRequest(
                accountId, fromDate, toDate, ReconciliationStrategy.STRICT
            );
            
            String result = request.toString();
            
            assertThat(result).contains("ReconciliationRequest");
            assertThat(result).contains(accountId.toString());
            assertThat(result).contains(fromDate.toString());
            assertThat(result).contains(toDate.toString());
            assertThat(result).contains("STRICT");
        }
    }
}