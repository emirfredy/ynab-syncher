package co.personal.ynabsyncher.infrastructure.memory;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of CategoryMappingRepository for exact text matching.
 * Uses concurrent data structures for thread safety.
 * Optimized for exact pattern lookups using normalized text.
 */
public class InMemoryCategoryMappingRepository implements CategoryMappingRepository {
    
    private final ConcurrentMap<String, CategoryMapping> mappingsById = new ConcurrentHashMap<>();
    
    @Override
    public List<CategoryMapping> findMappingsForPattern(TransactionPattern pattern) {
        return mappingsById.values().stream()
                .filter(mapping -> mapping.hasExactMatch(pattern))
                .sorted(Comparator
                    .comparing(CategoryMapping::confidence).reversed()
                    .thenComparing(CategoryMapping::occurrenceCount).reversed())
                .toList();
    }
    
    @Override
    public Optional<CategoryMapping> findBestMappingForPattern(TransactionPattern pattern) {
        return findMappingsForPattern(pattern).stream()
                .findFirst();
    }
    
    @Override
    public List<CategoryMapping> findMappingsForCategory(Category category) {
        return mappingsById.values().stream()
                .filter(mapping -> mapping.category().equals(category))
                .sorted(Comparator.comparing(CategoryMapping::confidence).reversed())
                .toList();
    }
    
    @Override
    public List<CategoryMapping> findMappingsContainingAnyPattern(List<String> textPatterns) {
        return mappingsById.values().stream()
                .filter(mapping -> mapping.textPatterns().stream()
                    .anyMatch(textPatterns::contains))
                .sorted(Comparator.comparing(CategoryMapping::confidence).reversed())
                .toList();
    }
    
    @Override
    public CategoryMapping save(CategoryMapping mapping) {
        mappingsById.put(mapping.id().value(), mapping);
        return mapping;
    }
    
    @Override
    public List<CategoryMapping> saveAll(List<CategoryMapping> mappings) {
        for (CategoryMapping mapping : mappings) {
            save(mapping);
        }
        return mappings;
    }
    
    /**
     * Clears all mappings. Used for testing.
     */
    public void clear() {
        mappingsById.clear();
    }
    
    /**
     * Returns the total number of stored mappings.
     */
    public int size() {
        return mappingsById.size();
    }
}