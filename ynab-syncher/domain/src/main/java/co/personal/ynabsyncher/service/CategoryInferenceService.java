package co.personal.ynabsyncher.service;

import co.personal.ynabsyncher.model.CategoryInferenceResult;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for inferring transaction categories based on transaction data.
 * Uses fuzzy matching algorithms and confidence scoring to map bank transactions
 * to appropriate YNAB categories.
 * 
 * Architecture: This service is framework-free and doesn't access SPI ports directly.
 * Categories are provided by the use case which handles repository access.
 */
public class CategoryInferenceService {
    
    private static final double MINIMUM_CONFIDENCE_THRESHOLD = 0.3;
    
    public CategoryInferenceService() {
        // Framework-free service - no dependencies
    }
    
    /**
     * Analyzes a bank transaction to find the most appropriate category.
     * Returns inference metadata without modifying the transaction.
     * 
     * @param transaction the bank transaction to analyze
     * @param availableCategories the YNAB categories to choose from
     * @return inference result with category and confidence, or empty if no match
     */
    public Optional<CategoryInferenceResult> analyzeTransaction(BankTransaction transaction, List<YnabCategory> availableCategories) {
        if (availableCategories.isEmpty()) {
            return Optional.empty();
        }
        
        return findBestMatch(transaction, availableCategories);
    }
    
    private Optional<CategoryInferenceResult> findBestMatch(BankTransaction transaction, List<YnabCategory> categories) {
        return categories.stream()
                .flatMap(category -> {
                    var merchantMatch = tryMerchantNameMatch(transaction, category);
                    var descriptionMatch = tryDescriptionMatch(transaction, category);
                    var expensePatternMatch = tryExpensePatternMatch(transaction, category);
                    
                    return List.of(merchantMatch, descriptionMatch, expensePatternMatch).stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get);
                })
                .filter(result -> result.confidence() >= MINIMUM_CONFIDENCE_THRESHOLD)
                .max(Comparator.comparing(CategoryInferenceResult::confidence));
    }
    
    private Optional<CategoryInferenceResult> tryMerchantNameMatch(BankTransaction transaction, YnabCategory category) {
        if (transaction.merchantName() == null || transaction.merchantName().isBlank()) {
            return Optional.empty();
        }
        
        String merchantName = transaction.merchantName().toLowerCase().trim();
        if (merchantName.length() < 3) {
            return Optional.empty(); // Too short for meaningful matching
        }
        
        double similarity = category.calculateSimilarityScore(merchantName);
        if (similarity < MINIMUM_CONFIDENCE_THRESHOLD) {
            return Optional.empty();
        }
        
        return Optional.of(new CategoryInferenceResult(
                Category.of(category.groupName(), category.name()),
                similarity,
                "Merchant name match: " + transaction.merchantName()
        ));
    }
    
    private Optional<CategoryInferenceResult> tryDescriptionMatch(BankTransaction transaction, YnabCategory category) {
        if (transaction.description() == null || transaction.description().isBlank()) {
            return Optional.empty();
        }
        
        String description = transaction.description().toLowerCase().trim();
        if (description.length() < 3) {
            return Optional.empty(); // Too short for meaningful matching
        }
        
        double similarity = category.calculateSimilarityScore(description);
        if (similarity < MINIMUM_CONFIDENCE_THRESHOLD) {
            return Optional.empty();
        }
        
        return Optional.of(new CategoryInferenceResult(
                Category.of(category.groupName(), category.name()),
                similarity * 0.9, // Slightly lower confidence for description vs merchant name
                "Description match: " + transaction.description()
        ));
    }
    
    private Optional<CategoryInferenceResult> tryExpensePatternMatch(BankTransaction transaction, YnabCategory category) {
        // Look for common expense patterns in transaction description
        String searchText = buildSearchText(transaction);
        if (searchText.length() < 3) {
            return Optional.empty();
        }
        
        // Check if any category keywords match the transaction text
        List<String> categoryKeywords = Arrays.asList(category.getInferenceKeywords());
        boolean hasKeywordMatch = categoryKeywords.stream()
                .anyMatch(keyword -> searchText.contains(keyword.toLowerCase()));

        if (!hasKeywordMatch) {
            return Optional.empty();
        }

        return Optional.of(new CategoryInferenceResult(
                Category.of(category.groupName(), category.name()),
                category.calculateSimilarityScore(searchText) * 0.8, // Lower confidence for expense pattern
                "Expense pattern match"
        ));
    }
    
    private String buildSearchText(BankTransaction transaction) {
        StringBuilder searchText = new StringBuilder();
        
        if (transaction.merchantName() != null && !transaction.merchantName().isBlank()) {
            searchText.append(transaction.merchantName()).append(" ");
        }
        
        if (transaction.description() != null && !transaction.description().isBlank()) {
            searchText.append(transaction.description()).append(" ");
        }
        
        return searchText.toString().toLowerCase().trim();
    }
}