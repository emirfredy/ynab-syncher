package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.Transaction;

import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a transaction reconciliation process.
 */
public record ReconciliationResult(
    List<Transaction> missingFromYnab,
    List<Transaction> matchedTransactions,
    ReconciliationSummary summary
) {
    public ReconciliationResult {
        Objects.requireNonNull(missingFromYnab, "Missing from YNAB list cannot be null");
        Objects.requireNonNull(matchedTransactions, "Matched transactions list cannot be null");
        Objects.requireNonNull(summary, "Summary cannot be null");
        
        // Ensure immutability
        missingFromYnab = List.copyOf(missingFromYnab);
        matchedTransactions = List.copyOf(matchedTransactions);
    }

    /**
     * Returns true if all bank transactions were found in YNAB (perfect reconciliation).
     */
    public boolean isFullyReconciled() {
        return missingFromYnab.isEmpty();
    }

    /**
     * Returns the number of transactions that need to be added to YNAB.
     */
    public int getMissingTransactionCount() {
        return missingFromYnab.size();
    }

    /**
     * Returns the number of transactions that were successfully matched.
     */
    public int getMatchedTransactionCount() {
        return matchedTransactions.size();
    }
}