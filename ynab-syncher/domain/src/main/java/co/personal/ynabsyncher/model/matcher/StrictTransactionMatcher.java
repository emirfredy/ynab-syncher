package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.Transaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

import java.util.Objects;

/**
 * Strict transaction matcher that requires exact match on amount, account, and date (day/month/year only).
 */
public class StrictTransactionMatcher implements TransactionMatcher {
    
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
        
        // Must have same date (day, month, year only - ignoring time)
        return Objects.equals(transaction1.date(), transaction2.date());
    }
    
    @Override
    public ReconciliationStrategy getStrategy() {
        return ReconciliationStrategy.STRICT;
    }
}