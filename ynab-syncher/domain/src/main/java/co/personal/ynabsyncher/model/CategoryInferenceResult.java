package co.personal.ynabsyncher.model;

/**
 * Domain model representing the result of category inference with confidence score and reasoning.
 * Contains the inferred category, confidence level, and explanation of the matching logic.
 * 
 * This is a proper domain concept that belongs in the model layer, not in API DTOs.
 */
public record CategoryInferenceResult(
    Category category,
    double confidence,
    String reasoning
) {
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    
    public CategoryInferenceResult {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Creates a result indicating no suitable category match was found.
     */
    public static CategoryInferenceResult noMatch() {
        return new CategoryInferenceResult(Category.unknown(), 0.0, "No suitable match found");
    }
    
    /**
     * Checks if a category match was found.
     */
    public boolean hasMatch() {
        return !category.equals(Category.unknown());
    }
    
    /**
     * Checks if the inference has high confidence.
     */
    public boolean isHighConfidence() {
        return confidence >= HIGH_CONFIDENCE_THRESHOLD;
    }
    
    /**
     * Creates a successful category inference result.
     */
    public static CategoryInferenceResult match(Category category, double confidence, String reasoning) {
        return new CategoryInferenceResult(category, confidence, reasoning);
    }
}