package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.TransactionId;

import java.util.List;
import java.util.Objects;

/**
 * Request DTO for category inference operation.
 * Contains transaction IDs that need category inference.
 */
public record CategoryInferenceRequest(
    List<TransactionId> transactionIds
) {
    public CategoryInferenceRequest {
        Objects.requireNonNull(transactionIds, "Transaction IDs cannot be null");
        if (transactionIds.isEmpty()) {
            throw new IllegalArgumentException("Transaction IDs cannot be empty");
        }
    }
    
    /**
     * Creates a request for a single transaction.
     */
    public static CategoryInferenceRequest forSingleTransaction(TransactionId transactionId) {
        return new CategoryInferenceRequest(List.of(transactionId));
    }
    
    /**
     * Gets the number of transactions to process.
     */
    public int getTransactionCount() {
        return transactionIds.size();
    }
}