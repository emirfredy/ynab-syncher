package co.personal.ynabsyncher.model.ynab;

/**
 * Represents the cleared status of a YNAB transaction.
 */
public enum ClearedStatus {
    CLEARED,     // Transaction is cleared
    UNCLEARED,   // Transaction is not cleared
    RECONCILED   // Transaction is reconciled
}