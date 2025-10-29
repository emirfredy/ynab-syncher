package co.personal.ynabsyncher.spi.repository;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.TransactionPattern;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving learned category mappings.
 * Maps transaction text patterns to categories using exact text matching.
 * Built from historical transaction categorization data.
 * 
 * Supports both read operations for inference and write operations for learning.
 */
public interface CategoryMappingRepository {
    
    // ========== WRITE OPERATIONS (Learning) ==========
    
    /**
     * Saves a category mapping. If a mapping with the same ID exists,
     * it will be updated; otherwise, a new mapping is created.
     * 
     * @param mapping the category mapping to save
     * @return the saved mapping
     * @throws IllegalArgumentException if mapping is null
     */
    CategoryMapping save(CategoryMapping mapping);
    
    /**
     * Saves multiple category mappings in a batch operation.
     * More efficient than individual save operations.
     * 
     * @param mappings the mappings to save
     * @return list of saved mappings in the same order
     * @throws IllegalArgumentException if mappings is null or empty
     */
    List<CategoryMapping> saveAll(List<CategoryMapping> mappings);
    
    // ========== READ OPERATIONS (Inference) ==========
    
    /**
     * Finds category mappings that have exact text matches with the given transaction pattern.
     * Returns mappings ordered by confidence and recency.
     * 
     * @param pattern the transaction pattern to match against
     * @return list of matching category mappings, empty if none found
     */
    List<CategoryMapping> findMappingsForPattern(TransactionPattern pattern);
    
    /**
     * Finds the most confident category mapping for a transaction pattern using exact matching.
     * 
     * @param pattern the transaction pattern to match against
     * @return the best matching category mapping, empty if none found
     */
    Optional<CategoryMapping> findBestMappingForPattern(TransactionPattern pattern);
    
    /**
     * Finds all learned mappings for a specific category.
     * Useful for understanding what text patterns lead to a category.
     * 
     * @param category the category to find mappings for
     * @return list of text patterns that map to this category
     */
    List<CategoryMapping> findMappingsForCategory(Category category);
    
    /**
     * Finds mappings that contain any of the specified text patterns.
     * Used for efficient exact text matching queries.
     * 
     * @param textPatterns the normalized text patterns to search for
     * @return list of mappings containing any of the text patterns
     */
    List<CategoryMapping> findMappingsContainingAnyPattern(List<String> textPatterns);
}