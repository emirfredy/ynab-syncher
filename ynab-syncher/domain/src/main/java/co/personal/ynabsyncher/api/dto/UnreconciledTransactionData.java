package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.CategoryId;
import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.Objects;
import java.util.Optional;

/**
 * Data representing a bank transaction that couldn't be reconciled with existing YNAB transactions
 * and needs to be created in YNAB, along with its inferred category information.
 */
public record UnreconciledTransactionData(
        BankTransaction bankTransaction,
        Optional<CategoryId> inferredCategoryId,
        double confidenceScore
) {
    public UnreconciledTransactionData {
        Objects.requireNonNull(bankTransaction, "Bank transaction cannot be null");
        
        if (inferredCategoryId == null) {
            inferredCategoryId = Optional.empty();
        }
        
        if (confidenceScore < 0.0 || confidenceScore > 1.0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0");
        }
    }
    
    public boolean hasHighConfidenceCategory() {
        return confidenceScore >= 0.8;
    }
    
    public boolean hasCategory() {
        return inferredCategoryId.isPresent();
    }
}