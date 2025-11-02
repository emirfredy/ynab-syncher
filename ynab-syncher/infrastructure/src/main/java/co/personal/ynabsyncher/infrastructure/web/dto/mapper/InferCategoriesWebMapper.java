package co.personal.ynabsyncher.infrastructure.web.dto.mapper;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.CategoryInferenceWebData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web DTO mapper for category inference operations.
 * 
 * Converts between Web DTOs (optimized for HTTP/JSON) and Domain DTOs.
 * Follows hexagonal architecture by mapping across layer boundaries.
 */
@Component
public class InferCategoriesWebMapper {

    /**
     * Converts web request to domain request.
     * TODO: Implement proper domain mapping when BankTransaction domain model is available
     */
    public CategoryInferenceRequest toDomainRequest(InferCategoriesWebRequest webRequest) {
        // Simplified implementation - needs proper domain mapping
        throw new UnsupportedOperationException("Domain mapping implementation needed");
    }

    /**
     * Converts domain response to web response.
     */
    public InferCategoriesWebResponse toWebResponse(CategoryInferenceResponse domainResponse) {
        List<CategoryInferenceWebData> inferences = domainResponse.results().stream()
                .filter(result -> result.successful())
                .map(result -> new CategoryInferenceWebData(
                        result.transactionId().value(),
                        result.inferenceResult().category().id().value(),
                        result.inferenceResult().category().name(),
                        BigDecimal.valueOf(result.inferenceResult().confidence()),
                        result.inferenceResult().reasoning()
                ))
                .toList();

        int lowConfidence = (int) domainResponse.results().stream()
                .filter(result -> result.successful() && 
                       result.inferenceResult().confidence() < 0.7)
                .count();

        return new InferCategoriesWebResponse(
                domainResponse.processedCount(),
                domainResponse.successfulInferences(),
                lowConfidence,
                inferences,
                List.of(), // warnings - TODO: extract from domain response
                List.of()  // errors - TODO: extract from domain response
        );
    }
}