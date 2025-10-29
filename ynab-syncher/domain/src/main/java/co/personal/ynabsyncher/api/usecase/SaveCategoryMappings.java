package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsRequest;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsResponse;

/**
 * Use case for persisting learned category mappings to improve future 
 * transaction categorization accuracy through machine learning.
 * 
 * This enables the system to learn from successful categorizations and
 * user corrections, building a knowledge base for automated categorization.
 * 
 * Integrates with existing CategoryMapping domain model to store patterns
 * that can be reused by the CategoryInferenceService for future transactions.
 */
public interface SaveCategoryMappings {
    
    /**
     * Saves category mappings learned from successful transaction categorizations.
     * 
     * Handles both new mappings and updates to existing mappings, including
     * confidence adjustments and pattern consolidation.
     * 
     * @param request containing the mappings to persist with metadata
     * @return result indicating which mappings were saved and any issues encountered
     * @throws IllegalArgumentException if request is invalid or contains malformed data
     */
    SaveCategoryMappingsResponse saveCategoryMappings(SaveCategoryMappingsRequest request);
}