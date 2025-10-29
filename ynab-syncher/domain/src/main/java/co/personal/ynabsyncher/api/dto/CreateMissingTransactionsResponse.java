package co.personal.ynabsyncher.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * Response from creating missing YNAB transactions.
 * Contains detailed results for each transaction creation attempt.
 */
public record CreateMissingTransactionsResponse(
        List<TransactionCreationResult> results,
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
    
    public static CreateMissingTransactionsResponse from(List<TransactionCreationResult> results) {
        Objects.requireNonNull(results, "Results cannot be null");
        
        int successful = (int) results.stream().mapToLong(r -> r.wasSuccessful() ? 1 : 0).sum();
        int failed = (int) results.stream().mapToLong(r -> r.wasSuccessful() ? 0 : 1).sum();
        
        return new CreateMissingTransactionsResponse(results, results.size(), successful, failed);
    }
    
    /**
     * Get only the successful transaction creation results.
     * Useful for tracking which bank transactions were successfully created in YNAB.
     */
    public List<TransactionCreationResult> getSuccessfulResults() {
        return results.stream()
                .filter(TransactionCreationResult::wasSuccessful)
                .toList();
    }
    
    /**
     * Get only the failed transaction creation results.
     * Useful for error reporting and retry logic.
     */
    public List<TransactionCreationResult> getFailedResults() {
        return results.stream()
                .filter(result -> !result.wasSuccessful())
                .toList();
    }
    
    /**
     * Check if there were any failures in the transaction creation process.
     */
    public boolean hasFailures() {
        return failed > 0;
    }
    
    /**
     * Check if all transactions were successfully created.
     */
    public boolean allSuccessful() {
        return failed == 0;
    }
}