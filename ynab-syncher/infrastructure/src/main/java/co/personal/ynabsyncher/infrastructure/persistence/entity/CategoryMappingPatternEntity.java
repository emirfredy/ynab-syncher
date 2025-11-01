package co.personal.ynabsyncher.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * JPA entity for category mapping pattern persistence.
 * Represents individual text patterns associated with category mappings.
 */
@Entity
@Table(name = "category_mapping_patterns", indexes = {
    @Index(name = "idx_category_mapping_pattern", columnList = "pattern")
})
@IdClass(CategoryMappingPatternId.class)
public class CategoryMappingPatternEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private CategoryMappingEntity mapping;

    @Id
    @Column(nullable = false, columnDefinition = "TEXT")
    private String pattern;

    // Default constructor for JPA
    protected CategoryMappingPatternEntity() {}

    public CategoryMappingPatternEntity(CategoryMappingEntity mapping, String pattern) {
        this.mapping = Objects.requireNonNull(mapping, "Mapping cannot be null");
        this.pattern = Objects.requireNonNull(pattern, "Pattern cannot be null");
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("Pattern cannot be blank");
        }
    }

    // Getters
    public CategoryMappingEntity getMapping() {
        return mapping;
    }

    public String getPattern() {
        return pattern;
    }

    // Setters for JPA
    public void setMapping(CategoryMappingEntity mapping) {
        this.mapping = mapping;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryMappingPatternEntity that = (CategoryMappingPatternEntity) o;
        return Objects.equals(mapping.getId(), that.mapping.getId()) && 
               Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapping.getId(), pattern);
    }

    @Override
    public String toString() {
        return "CategoryMappingPatternEntity{" +
                "mappingId=" + (mapping != null ? mapping.getId() : "null") +
                ", pattern='" + pattern + '\'' +
                '}';
    }
}