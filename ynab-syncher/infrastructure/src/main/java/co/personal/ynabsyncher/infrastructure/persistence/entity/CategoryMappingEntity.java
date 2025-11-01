package co.personal.ynabsyncher.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity for category mapping persistence.
 * Maps transaction text patterns to categories for ML learning.
 */
@Entity
@Table(name = "category_mappings", indexes = {
    @Index(name = "idx_category_mappings_category", columnList = "categoryId"),
    @Index(name = "idx_category_mappings_confidence", columnList = "confidence DESC")
})
public class CategoryMappingEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "category_id", nullable = false, length = 120)
    private String categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 50)
    private CategoryTypeEntity categoryType;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    @OneToMany(mappedBy = "mapping", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<CategoryMappingPatternEntity> patterns = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    protected CategoryMappingEntity() {}

    public CategoryMappingEntity(UUID id, String categoryId, String categoryName, 
                               CategoryTypeEntity categoryType, Double confidence, 
                               Integer occurrenceCount) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.categoryId = Objects.requireNonNull(categoryId, "Category ID cannot be null");
        this.categoryName = Objects.requireNonNull(categoryName, "Category name cannot be null");
        this.categoryType = Objects.requireNonNull(categoryType, "Category type cannot be null");
        this.confidence = Objects.requireNonNull(confidence, "Confidence cannot be null");
        this.occurrenceCount = Objects.requireNonNull(occurrenceCount, "Occurrence count cannot be null");
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public CategoryTypeEntity getCategoryType() {
        return categoryType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public Set<CategoryMappingPatternEntity> getPatterns() {
        return patterns;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters for updates
    public void setConfidence(Double confidence) {
        this.confidence = Objects.requireNonNull(confidence, "Confidence cannot be null");
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = Objects.requireNonNull(occurrenceCount, "Occurrence count cannot be null");
    }

    public void setPatterns(Set<CategoryMappingPatternEntity> patterns) {
        this.patterns.clear();
        if (patterns != null) {
            this.patterns.addAll(patterns);
            // Ensure bidirectional relationship
            this.patterns.forEach(pattern -> pattern.setMapping(this));
        }
    }

    public void addPattern(String patternText) {
        CategoryMappingPatternEntity pattern = new CategoryMappingPatternEntity(this, patternText);
        patterns.add(pattern);
    }

    public void removePattern(String patternText) {
        patterns.removeIf(pattern -> pattern.getPattern().equals(patternText));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryMappingEntity that = (CategoryMappingEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CategoryMappingEntity{" +
                "id=" + id +
                ", categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", categoryType=" + categoryType +
                ", confidence=" + confidence +
                ", occurrenceCount=" + occurrenceCount +
                ", patternCount=" + patterns.size() +
                '}';
    }
}