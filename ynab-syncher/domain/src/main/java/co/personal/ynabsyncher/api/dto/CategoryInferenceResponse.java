package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.TransactionId;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO for category inference operation.
 * Contains the results of category inference for each requested transaction.
 */
public record CategoryInferenceResponse(
    List<TransactionCategoryResult> results,
    int processedCount,
    int successfulInferences,
    int failedInferences
) {
    public CategoryInferenceResponse {
        Objects.requireNonNull(results, "Results cannot be null");
        if (processedCount < 0) {
            throw new IllegalArgumentException("Processed count cannot be negative");
        }
        if (successfulInferences < 0) {
            throw new IllegalArgumentException("Successful inferences count cannot be negative");
        }
        if (failedInferences < 0) {
            throw new IllegalArgumentException("Failed inferences count cannot be negative");
        }
    }
    
    /**
     * Individual transaction category inference result.
     */
    public record TransactionCategoryResult(
        TransactionId transactionId,
        CategoryInferenceResultDto inferenceResult,
        boolean successful
    ) {
        public TransactionCategoryResult {
            Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
            Objects.requireNonNull(inferenceResult, "Inference result cannot be null");
        }
        
        public static TransactionCategoryResult success(TransactionId transactionId, CategoryInferenceResultDto result) {
            return new TransactionCategoryResult(transactionId, result, result.hasMatch());
        }
        
        public static TransactionCategoryResult failure(TransactionId transactionId) {
            return new TransactionCategoryResult(transactionId, CategoryInferenceResultDto.noMatch(), false);
        }
    }
    
    /**
     * Gets the success rate as a percentage.
     */
    public double getSuccessRate() {
        if (processedCount == 0) return 0.0;
        return (double) successfulInferences / processedCount * 100.0;
    }
    
    /**
     * Checks if all inferences were successful.
     */
    public boolean allInferencesSuccessful() {
        return failedInferences == 0 && processedCount > 0;
    }
}