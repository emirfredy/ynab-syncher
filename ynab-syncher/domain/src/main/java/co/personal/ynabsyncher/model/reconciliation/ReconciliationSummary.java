package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.AccountId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Summary information about a reconciliation process.
 */
public record ReconciliationSummary(
    AccountId accountId,
    LocalDate reconciliationDate,
    LocalDate fromDate,
    LocalDate toDate,
    ReconciliationStrategy strategy,
    int totalBankTransactions,
    int totalYnabTransactions,
    int matchedTransactions,
    int missingFromYnab
) {
    public ReconciliationSummary {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(reconciliationDate, "Reconciliation date cannot be null");
        Objects.requireNonNull(fromDate, "From date cannot be null");
        Objects.requireNonNull(toDate, "To date cannot be null");
        Objects.requireNonNull(strategy, "Reconciliation strategy cannot be null");
        
        if (totalBankTransactions < 0) {
            throw new IllegalArgumentException("Total bank transactions cannot be negative");
        }
        if (totalYnabTransactions < 0) {
            throw new IllegalArgumentException("Total YNAB transactions cannot be negative");
        }
        if (matchedTransactions < 0) {
            throw new IllegalArgumentException("Matched transactions cannot be negative");
        }
        if (missingFromYnab < 0) {
            throw new IllegalArgumentException("Missing from YNAB cannot be negative");
        }
    }

    /**
     * Calculates the reconciliation percentage (matched / total bank transactions).
     */
    public double getReconciliationPercentage() {
        if (totalBankTransactions == 0) {
            return 100.0;
        }
        return (double) matchedTransactions / totalBankTransactions * 100.0;
    }

    /**
     * Returns true if reconciliation is complete (all bank transactions matched).
     */
    public boolean isComplete() {
        return missingFromYnab == 0;
    }
}