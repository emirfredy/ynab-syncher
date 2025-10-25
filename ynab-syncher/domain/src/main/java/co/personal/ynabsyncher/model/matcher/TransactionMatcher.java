package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.Transaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

/**
 * Interface for transaction matching strategies.
 */
public interface TransactionMatcher {
    
    /**
     * Determines if two transactions match according to this strategy.
     * 
     * @param transaction1 the first transaction
     * @param transaction2 the second transaction
     * @return true if the transactions match, false otherwise
     */
    boolean matches(Transaction transaction1, Transaction transaction2);
    
    /**
     * Returns the strategy type this matcher implements.
     */
    ReconciliationStrategy getStrategy();
}