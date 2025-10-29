package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.CategoryMapping;

import java.util.List;
import java.util.Objects;

/**
 * Response for category mapping save operations.
 * 
 * Provides detailed information about which mappings were successfully saved,
 * updated, or skipped, along with any warnings or issues encountered.
 */
public record SaveCategoryMappingsResponse(
        int totalRequested,
        int savedNew,
        int updatedExisting,
        int skipped,
        List<CategoryMapping> savedMappings,
        List<String> warnings,
        List<String> errors
) {
    public SaveCategoryMappingsResponse {
        if (totalRequested < 0) throw new IllegalArgumentException("Total requested cannot be negative");
        if (savedNew < 0) throw new IllegalArgumentException("Saved new cannot be negative");
        if (updatedExisting < 0) throw new IllegalArgumentException("Updated existing cannot be negative");
        if (skipped < 0) throw new IllegalArgumentException("Skipped cannot be negative");
        
        Objects.requireNonNull(savedMappings, "Saved mappings cannot be null");
        Objects.requireNonNull(warnings, "Warnings cannot be null");
        Objects.requireNonNull(errors, "Errors cannot be null");
        
        // Defensive copies
        savedMappings = List.copyOf(savedMappings);
        warnings = List.copyOf(warnings);
        errors = List.copyOf(errors);
        
        // Validation
        int expectedProcessed = savedNew + updatedExisting + skipped;
        if (totalRequested != expectedProcessed) {
            throw new IllegalArgumentException(
                String.format("Inconsistent counts: requested %d, processed %d", 
                    totalRequested, expectedProcessed)
            );
        }
    }
    
    /**
     * Creates a successful response with no issues.
     */
    public static SaveCategoryMappingsResponse success(
            int totalRequested,
            int savedNew,
            int updatedExisting,
            List<CategoryMapping> savedMappings) {
        return new SaveCategoryMappingsResponse(
            totalRequested,
            savedNew,
            updatedExisting,
            0, // no skipped
            savedMappings,
            List.of(), // no warnings
            List.of()  // no errors
        );
    }
    
    /**
     * Creates a partial success response with warnings.
     */
    public static SaveCategoryMappingsResponse partialSuccess(
            int totalRequested,
            int savedNew,
            int updatedExisting,
            int skipped,
            List<CategoryMapping> savedMappings,
            List<String> warnings) {
        return new SaveCategoryMappingsResponse(
            totalRequested,
            savedNew,
            updatedExisting,
            skipped,
            savedMappings,
            warnings,
            List.of() // no errors
        );
    }
    
    /**
     * Creates a failure response with errors.
     */
    public static SaveCategoryMappingsResponse failure(
            int totalRequested,
            List<String> errors) {
        return new SaveCategoryMappingsResponse(
            totalRequested,
            0, // no saved
            0, // no updated
            totalRequested, // all skipped
            List.of(), // no saved mappings
            List.of(), // no warnings
            errors
        );
    }
    
    /**
     * Checks if the operation was completely successful.
     */
    public boolean isCompleteSuccess() {
        return skipped == 0 && warnings.isEmpty() && errors.isEmpty();
    }
    
    /**
     * Checks if any mappings were successfully saved or updated.
     */
    public boolean hasSuccessfulOperations() {
        return savedNew > 0 || updatedExisting > 0;
    }
    
    /**
     * Returns the total number of mappings that were processed.
     */
    public int getTotalProcessed() {
        return savedNew + updatedExisting + skipped;
    }
    
    /**
     * Returns the total number of mappings that were persisted (saved or updated).
     */
    public int getTotalPersisted() {
        return savedNew + updatedExisting;
    }
    
    /**
     * Checks if there were any errors during processing.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Checks if there were any warnings during processing.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}