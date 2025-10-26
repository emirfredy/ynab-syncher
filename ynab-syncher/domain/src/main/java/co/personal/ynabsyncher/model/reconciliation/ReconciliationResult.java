package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a transaction reconciliation process.
 * Contains bank transactions that were matched and those missing from YNAB.
 */
public record ReconciliationResult(
    List<BankTransaction> missingFromYnab,
    List<BankTransaction> matchedTransactions,
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
     * Returns the number of transactions that were successfully matched.
     */
    public int getMatchedCount() {
        return matchedTransactions.size();
    }

    /**
     * Returns the number of transactions missing from YNAB.
     */
    public int getMissingCount() {
        return missingFromYnab.size();
    }

    /**
     * Returns the total number of bank transactions processed.
     */
    public int getTotalBankTransactions() {
        return matchedTransactions.size() + missingFromYnab.size();
    }
}