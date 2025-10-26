package co.personal.ynabsyncher.model.matcher;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;

/**
 * Transaction matcher that considers category similarity in addition to basic matching criteria.
 * This provides more sophisticated matching by considering whether transactions
 * are in similar categories, which increases confidence in matches.
 */
public abstract class CategoryAwareTransactionMatcher implements TransactionMatcher {
    
    /**
     * Performs basic matching (amount, account, date range) and then enhances
     * the decision with category analysis.
     */
    @Override
    public final boolean matches(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction) {
        // Fast pre-filter
        if (!bankTransaction.canPotentiallyMatch(ynabTransaction)) {
            return false;
        }
        
        // Delegate to specific date matching strategy
        if (!matchesDateCriteria(bankTransaction, ynabTransaction)) {
            return false;
        }
        
        // Enhance matching confidence with category analysis
        return enhanceMatchingWithCategory(bankTransaction, ynabTransaction);
    }
    
    /**
     * Subclasses implement their specific date matching logic.
     */
    protected abstract boolean matchesDateCriteria(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction);
    
    /**
     * Enhances matching decision by considering category similarity.
     * If categories are similar, it increases match confidence.
     * If categories are very different, it may decrease confidence.
     */
    protected boolean enhanceMatchingWithCategory(ReconcilableTransaction bankTransaction, ReconcilableTransaction ynabTransaction) {
        var bankCategory = bankTransaction.category();
        var ynabCategory = ynabTransaction.category();
        
        // If categories are similar, it's a strong positive signal
        if (bankCategory.isSimilarTo(ynabCategory)) {
            return true;
        }
        
        // If one is unknown, don't penalize (neutral)
        if (bankCategory.equals(Category.unknown()) || 
            ynabCategory.equals(Category.unknown())) {
            return true; // Neutral - proceed with basic matching
        }
        
        // If categories are explicitly different, be more cautious
        // This could indicate different transactions that happen to have same amount/date
        return areCategoriesCompatibleForMatching(bankCategory, ynabCategory);
    }
    
    /**
     * Determines if different categories are still compatible for matching.
     * This allows for cases where category inference might not be perfect
     * or where YNAB categories might be more specific than inferred ones.
     */
    protected boolean areCategoriesCompatibleForMatching(Category bankCategory, Category ynabCategory) {
        // For now, allow different categories (conservative approach)
        // This can be enhanced with category mapping rules or ML-based similarity
        return true;
    }
}