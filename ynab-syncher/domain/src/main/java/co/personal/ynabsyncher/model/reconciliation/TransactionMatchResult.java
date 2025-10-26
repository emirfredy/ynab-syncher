package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.List;
import java.util.Objects;

/**
 * Result of transaction matching operation containing matched and unmatched transactions.
 * This value object encapsulates the core matching results before summary creation.
 */
public record TransactionMatchResult(
    List<BankTransaction> matchedTransactions,
    List<BankTransaction> missingFromYnab
) {
    public TransactionMatchResult {
        Objects.requireNonNull(matchedTransactions, "Matched transactions cannot be null");
        Objects.requireNonNull(missingFromYnab, "Missing from YNAB transactions cannot be null");
    }

    /**
     * Returns the total number of matched transactions.
     */
    public int getMatchedCount() {
        return matchedTransactions.size();
    }

    /**
     * Returns the total number of transactions missing from YNAB.
     */
    public int getMissingFromYnabCount() {
        return missingFromYnab.size();
    }

    /**
     * Returns the total number of bank transactions processed.
     */
    public int getTotalBankTransactions() {
        return matchedTransactions.size() + missingFromYnab.size();
    }
}