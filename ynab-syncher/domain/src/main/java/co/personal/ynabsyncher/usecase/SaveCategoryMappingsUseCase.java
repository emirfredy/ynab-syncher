package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsRequest;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsResponse;
import co.personal.ynabsyncher.api.usecase.SaveCategoryMappings;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of SaveCategoryMappings use case.
 * 
 * Handles the persistence of learned category mappings with smart conflict resolution:
 * - Consolidates overlapping patterns for the same category
 * - Updates confidence scores based on repeated confirmations
 * - Detects and handles conflicting categorizations
 * - Validates mapping quality before persistence
 */
public class SaveCategoryMappingsUseCase implements SaveCategoryMappings {
    
    private final CategoryMappingRepository categoryMappingRepository;
    
    public SaveCategoryMappingsUseCase(CategoryMappingRepository categoryMappingRepository) {
        this.categoryMappingRepository = Objects.requireNonNull(
            categoryMappingRepository, "Category mapping repository cannot be null");
    }
    
    @Override
    public SaveCategoryMappingsResponse saveCategoryMappings(SaveCategoryMappingsRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        
        List<CategoryMapping> mappingsToSave = request.mappings();
        List<CategoryMapping> savedMappings = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        int savedNew = 0;
        int updatedExisting = 0;
        int skipped = 0;
        
        for (CategoryMapping mapping : mappingsToSave) {
            try {
                MappingProcessResult result = processMapping(mapping);
                
                switch (result.action()) {
                    case SAVE_NEW -> {
                        CategoryMapping saved = categoryMappingRepository.save(result.mapping());
                        savedMappings.add(saved);
                        savedNew++;
                    }
                    case UPDATE_EXISTING -> {
                        CategoryMapping updated = categoryMappingRepository.save(result.mapping());
                        savedMappings.add(updated);
                        updatedExisting++;
                    }
                    case SKIP -> {
                        skipped++;
                        if (result.reason() != null) {
                            warnings.add(result.reason());
                        }
                    }
                }
                
            } catch (Exception e) {
                errors.add("Failed to process mapping " + mapping.id() + ": " + e.getMessage());
                skipped++;
            }
        }
        
        if (!errors.isEmpty()) {
            return SaveCategoryMappingsResponse.failure(mappingsToSave.size(), errors);
        }
        
        if (!warnings.isEmpty() || skipped > 0) {
            return SaveCategoryMappingsResponse.partialSuccess(
                mappingsToSave.size(), savedNew, updatedExisting, skipped, savedMappings, warnings);
        }
        
        return SaveCategoryMappingsResponse.success(
            mappingsToSave.size(), savedNew, updatedExisting, savedMappings);
    }
    
    /**
     * Processes a single mapping to determine what action should be taken.
     */
    private MappingProcessResult processMapping(CategoryMapping mapping) {
        // Validate mapping quality
        if (!isValidMapping(mapping)) {
            return MappingProcessResult.skip("Mapping quality too low: " + mapping.confidence());
        }
        
        // Check for existing mappings with overlapping patterns
        TransactionPattern pattern = new TransactionPattern(mapping.textPatterns());
        List<CategoryMapping> existingMappings = categoryMappingRepository.findMappingsForPattern(pattern);
        
        if (existingMappings.isEmpty()) {
            // No conflicts - save as new
            return MappingProcessResult.saveNew(mapping);
        }
        
        // Handle conflicts and consolidation
        return handleExistingMappings(mapping, existingMappings);
    }
    
    /**
     * Handles the case where existing mappings overlap with the new mapping.
     */
    private MappingProcessResult handleExistingMappings(
            CategoryMapping newMapping, 
            List<CategoryMapping> existingMappings) {
        
        for (CategoryMapping existing : existingMappings) {
            // Same category - consolidate patterns and boost confidence
            if (existing.category().equals(newMapping.category())) {
                return consolidateMappings(existing, newMapping);
            }
            
            // Different category - potential conflict
            if (hasSignificantPatternOverlap(existing, newMapping)) {
                // Skip if existing mapping has much higher confidence
                if (existing.confidence() > newMapping.confidence() + 0.2) {
                    return MappingProcessResult.skip(
                        "Conflicting categorization exists with higher confidence: " + 
                        existing.category().name());
                }
                
                // If new mapping has higher confidence, we could replace,
                // but for safety, we'll skip and log a warning
                return MappingProcessResult.skip(
                    "Conflicting categorization detected - manual review needed");
            }
        }
        
        // No significant conflicts - save as new
        return MappingProcessResult.saveNew(newMapping);
    }
    
    /**
     * Consolidates two mappings for the same category.
     */
    private MappingProcessResult consolidateMappings(
            CategoryMapping existing, 
            CategoryMapping newMapping) {
        
        // Combine patterns
        Set<String> combinedPatterns = new HashSet<>(existing.textPatterns());
        combinedPatterns.addAll(newMapping.textPatterns());
        
        // Combine patterns and update occurrence count (which boosts confidence)
        CategoryMapping consolidated = existing
            .withAdditionalPatterns(newMapping.textPatterns())
            .withNewOccurrence();
        
        return MappingProcessResult.updateExisting(consolidated);
    }
    
    /**
     * Checks if two mappings have significant pattern overlap.
     */
    private boolean hasSignificantPatternOverlap(
            CategoryMapping mapping1, 
            CategoryMapping mapping2) {
        
        Set<String> patterns1 = mapping1.textPatterns();
        Set<String> patterns2 = mapping2.textPatterns();
        
        // Count overlapping patterns
        long overlapCount = patterns1.stream()
            .filter(patterns2::contains)
            .count();
        
        // Consider it significant if 50% or more of patterns overlap
        int minPatternCount = Math.min(patterns1.size(), patterns2.size());
        return overlapCount > 0 && (double) overlapCount / minPatternCount >= 0.5;
    }
    
    /**
     * Validates that a mapping meets quality thresholds.
     */
    private boolean isValidMapping(CategoryMapping mapping) {
        // Minimum confidence threshold
        if (mapping.confidence() < 0.1) {
            return false;
        }
        
        // Must have meaningful patterns
        if (mapping.textPatterns().isEmpty()) {
            return false;
        }
        
        // Patterns must not be too short or generic
        boolean hasValidPattern = mapping.textPatterns().stream()
            .anyMatch(pattern -> pattern.length() >= 3 && !isGenericPattern(pattern));
        
        return hasValidPattern;
    }
    
    /**
     * Checks if a pattern is too generic to be useful.
     */
    private boolean isGenericPattern(String pattern) {
        String normalized = pattern.toLowerCase().trim();
        return normalized.length() < 3 || 
               Set.of("pos", "atm", "fee", "inc", "llc", "ltd").contains(normalized);
    }
    
    /**
     * Result of processing a single mapping.
     */
    private record MappingProcessResult(
            ProcessAction action,
            CategoryMapping mapping,
            String reason
    ) {
        static MappingProcessResult saveNew(CategoryMapping mapping) {
            return new MappingProcessResult(ProcessAction.SAVE_NEW, mapping, null);
        }
        
        static MappingProcessResult updateExisting(CategoryMapping mapping) {
            return new MappingProcessResult(ProcessAction.UPDATE_EXISTING, mapping, null);
        }
        
        static MappingProcessResult skip(String reason) {
            return new MappingProcessResult(ProcessAction.SKIP, null, reason);
        }
    }
    
    /**
     * Actions that can be taken when processing a mapping.
     */
    private enum ProcessAction {
        SAVE_NEW,
        UPDATE_EXISTING,
        SKIP
    }
}