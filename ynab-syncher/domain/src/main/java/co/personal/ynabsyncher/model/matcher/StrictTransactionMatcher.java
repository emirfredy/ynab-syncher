package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

import java.util.Objects;

/**
 * Strict transaction matcher that requires exact match on amount, account, and date (day/month/year only).
 * Enhanced with category awareness for improved matching accuracy.
 */
public class StrictTransactionMatcher extends CategoryAwareTransactionMatcher {
    
    @Override
    protected boolean matchesDateCriteria(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction) {
        if (bankTransaction == null || ynabTransaction == null) {
            return false;
        }
        
        // Must have same date (day, month, year only - ignoring time)
        return Objects.equals(bankTransaction.date(), ynabTransaction.date());
    }
    
    @Override
    public ReconciliationStrategy getStrategy() {
        return ReconciliationStrategy.STRICT;
    }
}