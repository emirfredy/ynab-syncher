package co.personal.ynabsyncher.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for category mappings.
 */
public record CategoryMappingId(String value) {
    public CategoryMappingId {
        Objects.requireNonNull(value, "CategoryMappingId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CategoryMappingId value cannot be blank");
        }
    }
    
    public static CategoryMappingId of(String value) {
        return new CategoryMappingId(value);
    }
    
    public static CategoryMappingId generate() {
        return new CategoryMappingId(UUID.randomUUID().toString());
    }
    
    @Override
    public String toString() {
        return value;
    }
}