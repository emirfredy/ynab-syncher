package co.personal.ynabsyncher.model;

import java.util.Objects;
import java.util.Set;

/**
 * Dictionary-style mapping between text patterns and categories.
 * Uses exact text matching for high-precision categorization.
 * Built from successful historical categorizations.
 */
public record CategoryMapping(
    CategoryMappingId id,
    Category category,
    Set<String> textPatterns,
    double confidence,
    int occurrenceCount    
) {
    public CategoryMapping {
        Objects.requireNonNull(id, "Mapping ID cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(textPatterns, "Text patterns cannot be null");
        
        if (textPatterns.isEmpty()) {
            throw new IllegalArgumentException("Category mapping must have at least one text pattern");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        if (occurrenceCount < 1) {
            throw new IllegalArgumentException("Occurrence count must be at least 1");
        }
    }
    
    /**
     * Creates a new mapping from a successful categorization.
     */
    public static CategoryMapping fromSuccessfulMatch(
            TransactionPattern pattern, 
            Category category) {
        return new CategoryMapping(
            CategoryMappingId.generate(),
            category,
            Set.copyOf(pattern.textPatterns()), // Defensive copy
            1.0, // Start with full confidence
            1   // First occurrence
        );
    }
    
    /**
     * Checks if this mapping has exact match with transaction pattern.
     * No fuzzy matching - requires perfect text correspondence.
     */
    public boolean hasExactMatch(TransactionPattern candidate) {
        return candidate.textPatterns().stream()
                .anyMatch(candidatePattern -> textPatterns.contains(candidatePattern));
    }
    
    /**
     * Updates the mapping with a new occurrence.
     * Increases confidence based on repeated successful matches.
     */
    public CategoryMapping withNewOccurrence() {
        double confidenceBoost = Math.min(0.1, 0.1 / Math.sqrt(occurrenceCount));
        double newConfidence = Math.min(1.0, confidence + confidenceBoost);
        
        return new CategoryMapping(
            id,
            category,
            textPatterns,
            newConfidence,
            occurrenceCount + 1
        );
    }
    
    /**
     * Adds new text patterns from additional successful matches.
     */
    public CategoryMapping withAdditionalPatterns(Set<String> newPatterns) {
        Set<String> combinedPatterns = new java.util.HashSet<>(textPatterns);
        combinedPatterns.addAll(newPatterns);
        
        return new CategoryMapping(
            id,
            category,
            Set.copyOf(combinedPatterns),
            confidence,
            occurrenceCount
        );
    }
    
    /**
     * Checks if this mapping has high confidence for automatic categorization.
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8 && occurrenceCount >= 2;
    }
    
    /**
     * Returns the number of learned text patterns.
     */
    public int patternCount() {
        return textPatterns.size();
    }
}