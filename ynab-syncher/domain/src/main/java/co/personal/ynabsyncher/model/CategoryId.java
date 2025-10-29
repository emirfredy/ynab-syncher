package co.personal.ynabsyncher.model;

import java.util.Objects;

/**
 * Value object representing a unique category identifier.
 */
public record CategoryId(String value) {
    public CategoryId {
        Objects.requireNonNull(value, "Category ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Category ID value cannot be empty");
        }
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}