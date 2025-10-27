package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;

/**
 * Use case interface for inferring transaction categories.
 * This is an inbound port in the hexagonal architecture.
 */
public interface InferTransactionCategories {

    /**
     * Infers categories for the specified bank transactions.
     * Uses available YNAB categories and fuzzy matching algorithms
     * to assign appropriate categories to uncategorized transactions.
     * 
     * @param request the category inference request containing transaction IDs
     * @return the inference response with results for each transaction
     */
    CategoryInferenceResponse inferCategories(CategoryInferenceRequest request);
}