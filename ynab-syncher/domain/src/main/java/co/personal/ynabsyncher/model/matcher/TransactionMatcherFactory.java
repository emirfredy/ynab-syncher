package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;

/**
 * Factory for creating transaction matchers based on reconciliation strategy.
 */
public class TransactionMatcherFactory {
    
    /**
     * Creates a transaction matcher for the specified strategy.
     * 
     * @param strategy the reconciliation strategy
     * @return the appropriate transaction matcher
     * @throws IllegalArgumentException if the strategy is not supported
     */
    public static TransactionMatcher createMatcher(ReconciliationStrategy strategy) {
        return switch (strategy) {
            case STRICT -> new StrictTransactionMatcher();
            case RANGE -> new RangeTransactionMatcher();
        };
    }
}