package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryMappingEntity;
import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryTypeEntity;
import co.personal.ynabsyncher.infrastructure.persistence.jpa.CategoryMappingJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct JPA repository tests to verify entity mapping and queries.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.flyway.enabled=false"
})
@DisplayName("CategoryMappingJpaRepository Direct Tests")
class CategoryMappingJpaRepositoryDirectTest {

    @Autowired
    private CategoryMappingJpaRepository repository;

    @Test
    @DisplayName("should save and retrieve entity with patterns")
    void shouldSaveAndRetrieveEntityWithPatterns() {
        // Given
        UUID id = UUID.randomUUID();
        CategoryMappingEntity entity = new CategoryMappingEntity(
                id,
                "cat-123",
                "Coffee",
                CategoryTypeEntity.YNAB_ASSIGNED,
                0.95,
                3
        );
        entity.addPattern("starbucks");
        entity.addPattern("coffee shop");

        // When
        CategoryMappingEntity saved = repository.saveAndFlush(entity);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getCategoryId()).isEqualTo("cat-123");
        assertThat(saved.getCategoryName()).isEqualTo("Coffee");
        assertThat(saved.getCategoryType()).isEqualTo(CategoryTypeEntity.YNAB_ASSIGNED);
        assertThat(saved.getConfidence()).isEqualTo(0.95);
        assertThat(saved.getOccurrenceCount()).isEqualTo(3);
        assertThat(saved.getPatterns()).hasSize(2);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find entities by pattern")
    void shouldFindEntitiesByPattern() {
        // Given
        CategoryMappingEntity entity1 = new CategoryMappingEntity(
                UUID.randomUUID(),
                "cat-coffee",
                "Coffee",
                CategoryTypeEntity.YNAB_ASSIGNED,
                0.95,
                3
        );
        entity1.addPattern("starbucks");
        entity1.addPattern("coffee");

        CategoryMappingEntity entity2 = new CategoryMappingEntity(
                UUID.randomUUID(),
                "cat-gas",
                "Gas",
                CategoryTypeEntity.YNAB_ASSIGNED,
                0.85,
                5
        );
        entity2.addPattern("shell");
        entity2.addPattern("gas station");

        repository.save(entity1);
        repository.save(entity2);

        // When
        var results = repository.findByPatternsContainingAnyPattern(java.util.List.of("starbucks"));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryName()).isEqualTo("Coffee");
    }
}