package co.personal.ynabsyncher.api.dto;

import java.util.Objects;

/**
 * Framework-free DTO representing raw bank transaction data from CSV import.
 * Immutable data structure for transferring parsed CSV data into the domain.
 */
public record BankTransactionData(
    String date,
    String description, 
    String amount,
    String merchantName
) {
    
    public BankTransactionData {
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        // merchantName can be null - will be derived from description if needed
        
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (amount.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be blank");
        }
    }
}