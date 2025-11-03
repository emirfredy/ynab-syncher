package co.personal.ynabsyncher.infrastructure.web.dto;

import java.util.List;

/**
 * Web DTO for category inference responses.
 * 
 * Contains inferred categories with confidence scores and processing statistics.
 * Optimized for JSON serialization in HTTP responses.
 */
public record InferCategoriesWebResponse(
        
        int totalTransactions,
        
        int categoriesInferred,
        
        int lowConfidenceResults,
        
        List<CategoryInferenceWebData> categoryInferences,
        
        List<String> warnings,
        
        List<String> errors
) {
    
    /**
     * Creates a successful inference response.
     */
    public static InferCategoriesWebResponse success(
            int totalTransactions,
            int categoriesInferred,
            int lowConfidenceResults,
            List<CategoryInferenceWebData> categoryInferences) {
        return new InferCategoriesWebResponse(
                totalTransactions,
                categoriesInferred,
                lowConfidenceResults,
                categoryInferences,
                List.of(),
                List.of()
        );
    }
    
    /**
     * Creates a response with warnings.
     */
    public static InferCategoriesWebResponse withWarnings(
            int totalTransactions,
            int categoriesInferred,
            int lowConfidenceResults,
            List<CategoryInferenceWebData> categoryInferences,
            List<String> warnings) {
        return new InferCategoriesWebResponse(
                totalTransactions,
                categoriesInferred,
                lowConfidenceResults,
                categoryInferences,
                warnings,
                List.of()
        );
    }
}