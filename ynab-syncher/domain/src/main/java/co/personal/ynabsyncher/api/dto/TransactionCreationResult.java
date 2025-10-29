package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of attempting to create a YNAB transaction from a bank transaction.
 * Maintains reference to original transaction to preserve context and avoid data duplication.
 */
public record TransactionCreationResult(
        BankTransaction originalTransaction,
        Optional<TransactionId> ynabTransactionId,
        Optional<String> errorMessage
) {
    public TransactionCreationResult {
        Objects.requireNonNull(originalTransaction, "Original transaction cannot be null");
        Objects.requireNonNull(ynabTransactionId, "YNAB transaction ID optional cannot be null");
        Objects.requireNonNull(errorMessage, "Error message optional cannot be null");
        
        boolean hasYnabId = ynabTransactionId.isPresent();
        boolean hasError = errorMessage.isPresent();
        
        if (hasYnabId && hasError) {
            throw new IllegalArgumentException("Cannot have both YNAB ID and error message");
        }
        
        if (!hasYnabId && !hasError) {
            throw new IllegalArgumentException("Must have either YNAB ID or error message");
        }
    }
    
    public boolean wasSuccessful() {
        return ynabTransactionId.isPresent();
    }
    
    public static TransactionCreationResult success(
            BankTransaction transaction, 
            TransactionId ynabId) {
        Objects.requireNonNull(transaction, "Bank transaction cannot be null");
        Objects.requireNonNull(ynabId, "YNAB transaction ID cannot be null");
        
        return new TransactionCreationResult(
            transaction, 
            Optional.of(ynabId), 
            Optional.empty()
        );
    }
    
    public static TransactionCreationResult failure(
            BankTransaction transaction, 
            String error) {
        Objects.requireNonNull(transaction, "Bank transaction cannot be null");
        Objects.requireNonNull(error, "Error message cannot be null");
        
        if (error.isBlank()) {
            throw new IllegalArgumentException("Error message cannot be blank");
        }
        
        return new TransactionCreationResult(
            transaction, 
            Optional.empty(), 
            Optional.of(error)
        );
    }
}