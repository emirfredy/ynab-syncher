package co.personal.ynabsyncher.infrastructure.web.dto;

import java.math.BigDecimal;

/**
 * Web DTO representing a category inference result for a transaction.
 * 
 * Contains the inferred category information with confidence score.
 * Optimized for JSON serialization in HTTP responses.
 */
public record CategoryInferenceWebData(
        
        String transactionId,
        
        String inferredCategoryId,
        
        String inferredCategoryName,
        
        BigDecimal confidence,
        
        String reason
) {
}