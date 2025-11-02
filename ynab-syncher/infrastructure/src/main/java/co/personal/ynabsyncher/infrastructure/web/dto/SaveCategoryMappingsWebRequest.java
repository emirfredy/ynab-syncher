package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Web DTO for saving category mappings requests.
 * 
 * Optimized for HTTP transport with JSON-friendly types and Bean Validation.
 * Used by REST controllers to receive category mapping persistence requests.
 */
public record SaveCategoryMappingsWebRequest(
        
        @NotNull(message = "Category mappings list cannot be null")
        @NotEmpty(message = "Category mappings list cannot be empty")
        @Valid
        List<CategoryMappingWebData> categoryMappings
) {
}