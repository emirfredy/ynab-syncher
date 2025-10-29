package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.BudgetId;

import java.util.List;
import java.util.Objects;

/**
 * Request for creating missing YNAB transactions from unreconciled bank transactions.
 */
public record CreateMissingTransactionsRequest(
        BudgetId budgetId,
        AccountId bankAccountId,
        AccountId ynabAccountId,
        List<UnreconciledTransactionData> unreconciledTransactions
) {
    public CreateMissingTransactionsRequest {
        Objects.requireNonNull(budgetId, "Budget ID cannot be null");
        Objects.requireNonNull(bankAccountId, "Bank account ID cannot be null");
        Objects.requireNonNull(ynabAccountId, "YNAB account ID cannot be null");
        Objects.requireNonNull(unreconciledTransactions, "Unreconciled transactions cannot be null");
        
        if (unreconciledTransactions.isEmpty()) {
            throw new IllegalArgumentException("Unreconciled transactions cannot be empty");
        }
        
        if (unreconciledTransactions.size() > 100) {
            throw new IllegalArgumentException("Cannot create more than 100 transactions in a single request");
        }
        
        // Defensive copy
        unreconciledTransactions = List.copyOf(unreconciledTransactions);
    }
}