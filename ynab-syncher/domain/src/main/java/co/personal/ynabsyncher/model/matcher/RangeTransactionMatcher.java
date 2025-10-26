package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Range transaction matcher that allows matching transactions within a 3-day window.
 * Enhanced with category awareness for improved matching accuracy.
 */
public class RangeTransactionMatcher extends CategoryAwareTransactionMatcher {
    
    private static final long MAX_DATE_DIFFERENCE_DAYS = 3L;
    
    @Override
    protected boolean matchesDateCriteria(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction) {
        if (bankTransaction == null || ynabTransaction == null) {
            return false;
        }
        
        // Dates must be within 3 days of each other
        return isWithinDateRange(bankTransaction.date(), ynabTransaction.date());
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