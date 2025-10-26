package co.personal.ynabsyncher.model;

import java.util.Objects;

/**
 * Represents a transaction category that can be either assigned (YNAB) or inferred (Bank).
 * This abstraction allows both explicit YNAB categories and inferred bank categories
 * to participate in reconciliation logic.
 */
public record Category(
    String id,
    String name,
    CategoryType type
) {
    public Category {
        Objects.requireNonNull(id, "Category ID cannot be null");
        Objects.requireNonNull(name, "Category name cannot be null");
        Objects.requireNonNull(type, "Category type cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
    }

    /**
     * Creates a YNAB category with explicit assignment.
     */
    public static Category ynabCategory(String id, String name) {
        return new Category(id, name, CategoryType.YNAB_ASSIGNED);
    }

    /**
     * Creates an inferred category from bank transaction analysis.
     */
    public static Category inferredCategory(String name) {
        // Generate a consistent ID for inferred categories
        String id = "inferred_" + name.toLowerCase().replaceAll("\\s+", "_");
        return new Category(id, name, CategoryType.BANK_INFERRED);
    }

    /**
     * Creates an unknown/uncategorized category.
     */
    public static Category unknown() {
        return new Category("unknown", "Uncategorized", CategoryType.UNKNOWN);
    }

    /**
     * Checks if this category was explicitly assigned (YNAB) vs inferred (Bank).
     */
    public boolean isExplicitlyAssigned() {
        return type == CategoryType.YNAB_ASSIGNED;
    }

    /**
     * Checks if this category was inferred from transaction data.
     */
    public boolean isInferred() {
        return type == CategoryType.BANK_INFERRED;
    }

    /**
     * Checks if categories are conceptually similar for reconciliation.
     * This allows matching between YNAB "Groceries" and inferred "Food & Dining".
     */
    public boolean isSimilarTo(Category other) {
        if (this.equals(other)) {
            return true;
        }
        
        // Exact name match regardless of type
        if (this.name.equalsIgnoreCase(other.name)) {
            return true;
        }
        
        // Additional similarity logic can be added here
        // (e.g., semantic matching, category mapping rules)
        return false;
    }
}