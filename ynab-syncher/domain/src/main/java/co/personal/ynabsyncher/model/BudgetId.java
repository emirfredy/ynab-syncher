package co.personal.ynabsyncher.model;

import java.util.Objects;

/**
 * Value object representing a unique budget identifier.
 */
public record BudgetId(String value) {
    public BudgetId {
        Objects.requireNonNull(value, "Budget ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget ID value cannot be empty");
        }
    }

    public static BudgetId of(String value) {
        return new BudgetId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}