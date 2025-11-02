package co.personal.ynabsyncher.infrastructure.web.dto;

import java.util.List;

/**
 * Web DTO for category mapping save responses.
 * 
 * Contains processing statistics and results from category mapping persistence.
 * Optimized for JSON serialization in HTTP responses.
 */
public record SaveCategoryMappingsWebResponse(
        
        int totalMappings,
        
        int savedNew,
        
        int updatedExisting,
        
        int skipped,
        
        List<CategoryMappingWebData> savedMappings,
        
        List<String> warnings,
        
        List<String> errors,
        
        String status
) {
    
    /**
     * Creates a successful save response.
     */
    public static SaveCategoryMappingsWebResponse success(
            int totalMappings,
            int savedNew,
            int updatedExisting,
            List<CategoryMappingWebData> savedMappings) {
        return new SaveCategoryMappingsWebResponse(
                totalMappings,
                savedNew,
                updatedExisting,
                0,
                savedMappings,
                List.of(),
                List.of(),
                "SUCCESS"
        );
    }
    
    /**
     * Creates a partial success response with warnings.
     */
    public static SaveCategoryMappingsWebResponse partialSuccess(
            int totalMappings,
            int savedNew,
            int updatedExisting,
            int skipped,
            List<CategoryMappingWebData> savedMappings,
            List<String> warnings) {
        return new SaveCategoryMappingsWebResponse(
                totalMappings,
                savedNew,
                updatedExisting,
                skipped,
                savedMappings,
                warnings,
                List.of(),
                "PARTIAL_SUCCESS"
        );
    }
    
    /**
     * Creates a failure response with errors.
     */
    public static SaveCategoryMappingsWebResponse failure(
            int totalMappings,
            List<String> errors) {
        return new SaveCategoryMappingsWebResponse(
                totalMappings,
                0,
                0,
                totalMappings,
                List.of(),
                List.of(),
                errors,
                "FAILURE"
        );
    }
}