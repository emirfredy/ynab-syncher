package co.personal.ynabsyncher.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * Request DTO for importing bank transactions from external data sources.
 */
public record ImportBankTransactionsRequest(
    String accountId,
    List<BankTransactionData> transactions
) {
    
    public ImportBankTransactionsRequest {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");
        
        if (accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be blank");
        }
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Transactions list cannot be empty");
        }
    }
}