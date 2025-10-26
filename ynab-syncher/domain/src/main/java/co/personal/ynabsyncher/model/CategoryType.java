package co.personal.ynabsyncher.model;

/**
 * Represents the type/source of a category assignment.
 */
public enum CategoryType {
    /**
     * Category explicitly assigned in YNAB by the user.
     */
    YNAB_ASSIGNED,
    
    /**
     * Category inferred from bank transaction description/merchant.
     */
    BANK_INFERRED,
    
    /**
     * Unknown or uncategorized transaction.
     */
    UNKNOWN
}