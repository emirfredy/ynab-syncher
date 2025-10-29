package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.CategoryMapping;

import java.util.List;
import java.util.Objects;

/**
 * Request for saving learned category mappings from successful categorizations.
 */
public record SaveCategoryMappingsRequest(
        List<CategoryMapping> mappings
) {
    public SaveCategoryMappingsRequest {
        Objects.requireNonNull(mappings, "Mappings cannot be null");
        
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("Mappings cannot be empty");
        }
        
        if (mappings.size() > 500) {
            throw new IllegalArgumentException("Cannot save more than 500 mappings in a single request");
        }
        
        // Defensive copy
        mappings = List.copyOf(mappings);
    }
    
    /**
     * Returns the number of mappings in this request.
     */
    public int getMappingCount() {
        return mappings.size();
    }
}