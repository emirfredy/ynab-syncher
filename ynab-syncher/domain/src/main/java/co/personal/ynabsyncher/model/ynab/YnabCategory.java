package co.personal.ynabsyncher.model.ynab;

import java.util.Objects;

/**
 * Represents a YNAB category with hierarchical information for inference matching.
 * Contains additional metadata beyond the basic Category model to support
 * sophisticated category inference algorithms.
 */
public record YnabCategory(
    String id,
    String name,
    String groupId,
    String groupName,
    boolean isHidden,
    boolean isDeleted
) {
    public YnabCategory {
        Objects.requireNonNull(id, "Category ID cannot be null");
        Objects.requireNonNull(name, "Category name cannot be null");
        Objects.requireNonNull(groupId, "Category group ID cannot be null");
        Objects.requireNonNull(groupName, "Category group name cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
    }

    /**
     * Checks if this category is available for inference (not hidden or deleted).
     */
    public boolean isAvailableForInference() {
        return !isHidden && !isDeleted;
    }

    /**
     * Gets the full category name including group for disambiguation.
     * E.g., "Housing: Rent" or "Transportation: Gas"
     */
    public String getFullName() {
        return groupName + ": " + name;
    }

    /**
     * Generates keywords that might match this category in transaction descriptions.
     * This supports fuzzy matching by providing alternative terms.
     */
    public String[] getInferenceKeywords() {
        return new String[]{
            name.toLowerCase(),
            groupName.toLowerCase(),
            getFullName().toLowerCase()
        };
    }

    /**
     * Calculates similarity score with transaction text.
     * Higher scores indicate better matches for inference.
     */
    public double calculateSimilarityScore(String transactionText) {
        if (transactionText == null || transactionText.isBlank()) {
            return 0.0;
        }
        
        String lowerText = transactionText.toLowerCase();
        double maxScore = 0.0;
        
        // Exact name match gets highest score
        if (lowerText.contains(name.toLowerCase())) {
            maxScore = Math.max(maxScore, 1.0);
        }
        
        // Group name match gets medium score
        if (lowerText.contains(groupName.toLowerCase())) {
            maxScore = Math.max(maxScore, 0.7);
        }
        
        // Partial matches get lower scores
        String[] words = name.toLowerCase().split("\\s+");
        for (String word : words) {
            if (word.length() >= 3 && lowerText.contains(word)) {
                maxScore = Math.max(maxScore, 0.5);
            }
        }
        
        return maxScore;
    }
}