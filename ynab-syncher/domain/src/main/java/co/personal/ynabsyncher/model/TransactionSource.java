package co.personal.ynabsyncher.model;

/**
 * Enumeration representing the source of a transaction.
 */
public enum TransactionSource {
    /**
     * Transaction comes from YNAB.
     */
    YNAB,
    
    /**
     * Transaction comes from a bank or financial institution.
     */
    BANK
}