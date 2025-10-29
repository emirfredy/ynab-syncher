package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.TransactionId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Result of creating a transaction in YNAB.
 */
public record CreatedTransactionResult(
        TransactionId transactionId,
        String description,
        BigDecimal amount,
        LocalDate date,
        boolean wasSuccessful,
        String errorMessage
) {
    public CreatedTransactionResult {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
        
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        
        if (!wasSuccessful && (errorMessage == null || errorMessage.isBlank())) {
            throw new IllegalArgumentException("Error message is required when transaction creation was not successful");
        }
        
        if (wasSuccessful && errorMessage != null) {
            throw new IllegalArgumentException("Error message should be null when transaction creation was successful");
        }
    }
    
    public static CreatedTransactionResult success(
            TransactionId transactionId,
            String description,
            BigDecimal amount,
            LocalDate date
    ) {
        return new CreatedTransactionResult(transactionId, description, amount, date, true, null);
    }
    
    public static CreatedTransactionResult failure(
            TransactionId transactionId,
            String description,
            BigDecimal amount,
            LocalDate date,
            String errorMessage
    ) {
        return new CreatedTransactionResult(transactionId, description, amount, date, false, errorMessage);
    }
}