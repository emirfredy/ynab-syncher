package co.personal.ynabsyncher.infrastructure.web.dto.mapper;

import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsRequest;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.CategoryMappingWebData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web DTO mapper for category mapping operations.
 * 
 * Converts between Web DTOs (optimized for HTTP/JSON) and Domain DTOs.
 * Follows hexagonal architecture by mapping across layer boundaries.
 */
@Component
public class SaveCategoryMappingsWebMapper {

    /**
     * Converts web request to domain request.
     * TODO: Implement proper domain mapping when CategoryMapping domain model is available
     */
    public SaveCategoryMappingsRequest toDomainRequest(SaveCategoryMappingsWebRequest webRequest) {
        // Simplified implementation - needs proper domain mapping
        throw new UnsupportedOperationException("Domain mapping implementation needed");
    }

    /**
     * Converts domain response to web response.
     */
    public SaveCategoryMappingsWebResponse toWebResponse(SaveCategoryMappingsResponse domainResponse) {
        // Map saved mappings to web format
        List<CategoryMappingWebData> savedMappings = domainResponse.savedMappings().stream()
                .map(mapping -> new CategoryMappingWebData(
                        mapping.id().value(),
                        mapping.category().id().value(),
                        mapping.category().name(),
                        List.copyOf(mapping.textPatterns()),
                        BigDecimal.valueOf(mapping.confidence()),
                        "ML_INFERENCE" // simplified source
                ))
                .toList();

        // Determine response type based on domain response status
        if (domainResponse.isCompleteSuccess()) {
            return SaveCategoryMappingsWebResponse.success(
                    domainResponse.totalRequested(),
                    domainResponse.savedNew(),
                    domainResponse.updatedExisting(),
                    savedMappings
            );
        } else if (domainResponse.hasSuccessfulOperations()) {
            return SaveCategoryMappingsWebResponse.partialSuccess(
                    domainResponse.totalRequested(),
                    domainResponse.savedNew(),
                    domainResponse.updatedExisting(),
                    domainResponse.skipped(),
                    savedMappings,
                    domainResponse.warnings()
            );
        } else {
            return SaveCategoryMappingsWebResponse.failure(
                    domainResponse.totalRequested(),
                    domainResponse.errors()
            );
        }
    }
}