package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

/**
 * Interface for transaction matching strategies.
 * Determines whether two transactions should be considered the same.
 */
public interface TransactionMatcher {
    
    /**
     * Determines if a bank transaction matches a YNAB transaction.
     *
     * @param bankTransaction the bank transaction
     * @param ynabTransaction the YNAB transaction
     * @return true if the transactions match, false otherwise
     */
    boolean matches(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction);
    
    /**
     * Returns the strategy type this matcher implements.
     */
    ReconciliationStrategy getStrategy();
}