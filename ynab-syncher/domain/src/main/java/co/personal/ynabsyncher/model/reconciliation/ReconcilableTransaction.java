package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.TransactionSource;

import java.time.LocalDate;

/**
 * Common interface for transactions that can be reconciled.
 * Provides the complete data needed for sophisticated transaction matching,
 * including category information for enhanced reconciliation accuracy.
 */
public interface ReconcilableTransaction {
    
    TransactionId id();
    AccountId accountId();
    LocalDate date();
    Money amount();
    String displayName();
    Category category();
    TransactionSource source();
    
    /**
     * Gets additional context for reconciliation (memo, description, etc.).
     */
    String reconciliationContext();
    
    /**
     * Checks if this transaction can potentially match another based on core criteria.
     * This is a fast pre-filter before applying detailed matching strategies.
     */
    default boolean canPotentiallyMatch(ReconcilableTransaction other) {
        return this.accountId().equals(other.accountId()) 
            && this.amount().equals(other.amount())
            && !this.source().equals(other.source()); // Different sources only
    }
}