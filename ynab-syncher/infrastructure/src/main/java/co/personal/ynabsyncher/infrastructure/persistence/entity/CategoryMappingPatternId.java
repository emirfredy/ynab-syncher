package co.personal.ynabsyncher.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite ID class for CategoryMappingPatternEntity.
 * Required for JPA composite primary key mapping.
 */
public class CategoryMappingPatternId implements Serializable {

    private UUID mapping;
    private String pattern;

    // Default constructor for JPA
    public CategoryMappingPatternId() {}

    public CategoryMappingPatternId(UUID mapping, String pattern) {
        this.mapping = mapping;
        this.pattern = pattern;
    }

    // Getters and setters
    public UUID getMapping() {
        return mapping;
    }

    public void setMapping(UUID mapping) {
        this.mapping = mapping;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryMappingPatternId that = (CategoryMappingPatternId) o;
        return Objects.equals(mapping, that.mapping) && 
               Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapping, pattern);
    }

    @Override
    public String toString() {
        return "CategoryMappingPatternId{" +
                "mapping=" + mapping +
                ", pattern='" + pattern + '\'' +
                '}';
    }
}