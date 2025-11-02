package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web DTO representing a category mapping for ML learning.
 * 
 * Contains transaction patterns mapped to categories with confidence scores.
 * Optimized for JSON serialization in HTTP requests/responses.
 */
public record CategoryMappingWebData(
        
        String id,
        
        @NotBlank(message = "Category ID cannot be blank")
        String categoryId,
        
        @NotBlank(message = "Category name cannot be blank")
        String categoryName,
        
        @NotNull(message = "Text patterns cannot be null")
        List<String> textPatterns,
        
        @NotNull(message = "Confidence cannot be null")
        @DecimalMin(value = "0.0", message = "Confidence must be at least 0.0")
        @DecimalMax(value = "1.0", message = "Confidence must be at most 1.0")
        BigDecimal confidence,
        
        String source
) {
}