package co.personal.ynabsyncher.model.reconciliation;

/**
 * Enumeration of available reconciliation strategies.
 */
public enum ReconciliationStrategy {
    /**
     * Strict matching: transactions must have exactly the same amount, date, and account.
     * Date comparison is done by day, month, and year only (ignoring time).
     */
    STRICT,
    
    /**
     * Range matching: transactions are considered matching if they have the same amount
     * and account, and their dates are within 3 days of each other.
     */
    RANGE
}