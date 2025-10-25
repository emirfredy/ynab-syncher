package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.Transaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Range transaction matcher that allows matching transactions within a 3-day window.
 */
public class RangeTransactionMatcher implements TransactionMatcher {
    
    private static final long MAX_DATE_DIFFERENCE_DAYS = 3L;
    
    @Override
    public boolean matches(Transaction transaction1, Transaction transaction2) {
        if (transaction1 == null || transaction2 == null) {
            return false;
        }
        
        // Must have same amount
        if (!Objects.equals(transaction1.amount(), transaction2.amount())) {
            return false;
        }
        
        // Must have same account
        if (!Objects.equals(transaction1.accountId(), transaction2.accountId())) {
            return false;
        }
        
        // Dates must be within 3 days of each other
        return isWithinDateRange(transaction1.date(), transaction2.date());
    }
    
    private boolean isWithinDateRange(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        long daysBetween = Math.abs(ChronoUnit.DAYS.between(date1, date2));
        return daysBetween <= MAX_DATE_DIFFERENCE_DAYS;
    }
    
    @Override
    public ReconciliationStrategy getStrategy() {
        return ReconciliationStrategy.RANGE;
    }
}