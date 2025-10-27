package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.Category;

/**
 * API DTO for category inference results.
 * This is a data transfer object for external communication boundaries.
 * The actual domain logic resides in co.personal.ynabsyncher.model.CategoryInferenceResult.
 */
public record CategoryInferenceResultDto(
    Category category,
    double confidence,
    String reasoning
) {
    public CategoryInferenceResultDto {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Creates a DTO from a domain model.
     */
    public static CategoryInferenceResultDto fromDomain(co.personal.ynabsyncher.model.CategoryInferenceResult domainResult) {
        return new CategoryInferenceResultDto(
            domainResult.category(),
            domainResult.confidence(),
            domainResult.reasoning()
        );
    }

    public static CategoryInferenceResultDto noMatch() {
        return new CategoryInferenceResultDto(Category.unknown(), 0.0, "No suitable match found");
    }
    
    public boolean hasMatch() {
        return !category.equals(Category.unknown());
    }

}