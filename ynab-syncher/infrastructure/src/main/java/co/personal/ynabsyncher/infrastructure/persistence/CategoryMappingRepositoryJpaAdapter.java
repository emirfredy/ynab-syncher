package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryMappingEntity;
import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryMappingPatternEntity;
import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryTypeEntity;
import co.personal.ynabsyncher.infrastructure.persistence.jpa.CategoryMappingJpaRepository;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link CategoryMappingRepository}.
 * Provides persistence for category mappings using Spring Data JPA.
 * 
 * Handles entity-to-domain mapping and ensures proper transaction boundaries.
 */
@Repository
@Transactional
public class CategoryMappingRepositoryJpaAdapter implements CategoryMappingRepository {

    private final CategoryMappingJpaRepository jpaRepository;

    public CategoryMappingRepositoryJpaAdapter(CategoryMappingJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "JPA repository cannot be null");
    }

    @Override
    public CategoryMapping save(CategoryMapping mapping) {
        Objects.requireNonNull(mapping, "Category mapping cannot be null");
        
        CategoryMappingEntity entity = toEntity(mapping);
        CategoryMappingEntity savedEntity = jpaRepository.save(entity);
        
        return toDomain(savedEntity);
    }

    @Override
    public List<CategoryMapping> saveAll(List<CategoryMapping> mappings) {
        Objects.requireNonNull(mappings, "Mappings list cannot be null");
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("Mappings list cannot be empty");
        }

        List<CategoryMappingEntity> entities = mappings.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        List<CategoryMappingEntity> savedEntities = jpaRepository.saveAll(entities);

        return savedEntities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryMapping> findMappingsForPattern(TransactionPattern pattern) {
        Objects.requireNonNull(pattern, "Transaction pattern cannot be null");

        List<String> patterns = new ArrayList<>(pattern.textPatterns());
        if (patterns.isEmpty()) {
            return List.of();
        }

        List<CategoryMappingEntity> entities = jpaRepository.findByPatternsContainingAnyPattern(patterns);
        
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryMapping> findBestMappingForPattern(TransactionPattern pattern) {
        Objects.requireNonNull(pattern, "Transaction pattern cannot be null");

        List<String> patterns = new ArrayList<>(pattern.textPatterns());
        if (patterns.isEmpty()) {
            return Optional.empty();
        }

        CategoryMappingEntity entity = jpaRepository.findBestMappingForPatterns(patterns);
        
        return entity != null ? Optional.of(toDomain(entity)) : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryMapping> findMappingsForCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");

        List<CategoryMappingEntity> entities = jpaRepository
                .findByCategoryIdOrderByConfidenceDesc(category.id().value());

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryMapping> findMappingsContainingAnyPattern(List<String> textPatterns) {
        Objects.requireNonNull(textPatterns, "Text patterns cannot be null");
        if (textPatterns.isEmpty()) {
            return List.of();
        }

        List<CategoryMappingEntity> entities = jpaRepository.findByPatternsContainingAnyPattern(textPatterns);
        
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ========== ENTITY-DOMAIN MAPPING ==========

    private CategoryMappingEntity toEntity(CategoryMapping domain) {
        UUID entityId = UUID.fromString(domain.id().value());
        
        CategoryMappingEntity entity = new CategoryMappingEntity(
                entityId,
                domain.category().id().value(),
                domain.category().name(),
                CategoryTypeEntity.fromDomainType(domain.category().type()),
                domain.confidence(),
                domain.occurrenceCount()
        );

        // Add patterns
        Set<CategoryMappingPatternEntity> patternEntities = domain.textPatterns().stream()
                .map(pattern -> new CategoryMappingPatternEntity(entity, pattern))
                .collect(Collectors.toSet());
        
        entity.setPatterns(patternEntities);

        return entity;
    }

    private CategoryMapping toDomain(CategoryMappingEntity entity) {
        CategoryMappingId mappingId = CategoryMappingId.of(entity.getId().toString());
        
        Category category = new Category(
                CategoryId.of(entity.getCategoryId()),
                entity.getCategoryName(),
                entity.getCategoryType().toDomainType()
        );

        Set<String> textPatterns = entity.getPatterns().stream()
                .map(CategoryMappingPatternEntity::getPattern)
                .collect(Collectors.toSet());

        return new CategoryMapping(
                mappingId,
                category,
                textPatterns,
                entity.getConfidence(),
                entity.getOccurrenceCount()
        );
    }
}