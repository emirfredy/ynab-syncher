package co.personal.ynabsyncher.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * Response from creating missing YNAB transactions.
 */
public record CreateMissingTransactionsResponse(
        List<CreatedTransactionResult> results,
        int totalProcessed,
        int successfullyCreated,
        int failed
) {
    public CreateMissingTransactionsResponse {
        Objects.requireNonNull(results, "Results cannot be null");
        
        if (totalProcessed < 0) {
            throw new IllegalArgumentException("Total processed cannot be negative");
        }
        
        if (successfullyCreated < 0) {
            throw new IllegalArgumentException("Successfully created count cannot be negative");
        }
        
        if (failed < 0) {
            throw new IllegalArgumentException("Failed count cannot be negative");
        }
        
        if (totalProcessed != successfullyCreated + failed) {
            throw new IllegalArgumentException("Total processed must equal sum of successful and failed");
        }
        
        if (results.size() != totalProcessed) {
            throw new IllegalArgumentException("Results size must match total processed count");
        }
        
        // Defensive copy
        results = List.copyOf(results);
        
        // Validate counts match actual results
        long actualSuccessful = results.stream().mapToLong(r -> r.wasSuccessful() ? 1 : 0).sum();
        long actualFailed = results.stream().mapToLong(r -> r.wasSuccessful() ? 0 : 1).sum();
        
        if (actualSuccessful != successfullyCreated) {
            throw new IllegalArgumentException("Successfully created count does not match actual successful results");
        }
        
        if (actualFailed != failed) {
            throw new IllegalArgumentException("Failed count does not match actual failed results");
        }
    }
    
    public static CreateMissingTransactionsResponse from(List<CreatedTransactionResult> results) {
        Objects.requireNonNull(results, "Results cannot be null");
        
        int successful = (int) results.stream().mapToLong(r -> r.wasSuccessful() ? 1 : 0).sum();
        int failed = (int) results.stream().mapToLong(r -> r.wasSuccessful() ? 0 : 1).sum();
        
        return new CreateMissingTransactionsResponse(results, results.size(), successful, failed);
    }
}