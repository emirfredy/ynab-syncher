package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.BudgetId;
import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.List;
import java.util.Objects;

/**
 * Request for creating missing YNAB transactions from bank transactions that are missing from YNAB.
 * Typically populated from ReconciliationResult.missingFromYnab().
 */
public record CreateMissingTransactionsRequest(
        BudgetId budgetId,
        AccountId bankAccountId,
        AccountId ynabAccountId,
        List<BankTransaction> missingTransactions
) {
    public CreateMissingTransactionsRequest {
        Objects.requireNonNull(budgetId, "Budget ID cannot be null");
        Objects.requireNonNull(bankAccountId, "Bank account ID cannot be null");
        Objects.requireNonNull(ynabAccountId, "YNAB account ID cannot be null");
        Objects.requireNonNull(missingTransactions, "Missing transactions cannot be null");
        
        if (missingTransactions.isEmpty()) {
            throw new IllegalArgumentException("Missing transactions cannot be empty");
        }
        
        if (missingTransactions.size() > 100) {
            throw new IllegalArgumentException("Cannot create more than 100 transactions in a single request");
        }
        
        // Defensive copy
        missingTransactions = List.copyOf(missingTransactions);
    }
}