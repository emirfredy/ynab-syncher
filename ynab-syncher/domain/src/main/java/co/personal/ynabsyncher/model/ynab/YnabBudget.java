package co.personal.ynabsyncher.model.ynab;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represents a YNAB budget with metadata for API operations.
 * Contains budget-level information needed for YNAB API interactions.
 */
public record YnabBudget(
    String id,
    String name,
    OffsetDateTime lastModifiedOn,
    OffsetDateTime firstMonth,
    OffsetDateTime lastMonth,
    String currencyFormat
) {
    public YnabBudget {
        Objects.requireNonNull(id, "Budget ID cannot be null");
        Objects.requireNonNull(name, "Budget name cannot be null");
        Objects.requireNonNull(lastModifiedOn, "Last modified date cannot be null");
        Objects.requireNonNull(currencyFormat, "Currency format cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("Budget ID cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Budget name cannot be blank");
        }
    }

    /**
     * Checks if this budget is active (has recent modifications).
     * Considers a budget active if modified within the last 90 days.
     */
    public boolean isActive() {
        return lastModifiedOn.isAfter(OffsetDateTime.now().minusDays(90));
    }

    /**
     * Gets the display name for the budget.
     */
    public String displayName() {
        return name;
    }
}