package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.CategoryMappingId;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = CategoryMappingRepositoryJdbcAdapterTest.TestApplication.class)
@ActiveProfiles("test")
class CategoryMappingRepositoryJdbcAdapterTest extends CategoryMappingRepositoryContractTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
        @org.springframework.context.annotation.Bean
        CategoryMappingRepositoryJdbcAdapter categoryMappingRepositoryJdbcAdapter(
                NamedParameterJdbcTemplate jdbcTemplate
        ) {
            return new CategoryMappingRepositoryJdbcAdapter(jdbcTemplate);
        }
    }

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("ynab_syncher_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired
    private CategoryMappingRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    protected CategoryMappingRepository repository() {
        return repository;
    }

    @Override
    protected void clearRepository() {
        namedParameterJdbcTemplate.getJdbcTemplate().update("DELETE FROM category_mapping_patterns");
        namedParameterJdbcTemplate.getJdbcTemplate().update("DELETE FROM category_mappings");
    }

    @Test
    void saveShouldPersistAndRetrieveByPattern() {
        CategoryMapping mapping = repository.save(createMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory("food", "Groceries"),
                0.85,
                3,
                "merchant alpha", "alpha groceries"
        ));

        TransactionPattern pattern = new TransactionPattern(Set.of("merchant alpha"));
        List<CategoryMapping> results = repository.findMappingsForPattern(pattern);

        assertThat(results)
                .hasSize(1)
                .first()
                .satisfies(found -> {
                    assertThat(found.id()).isEqualTo(mapping.id());
                    assertThat(found.category().id()).isEqualTo(mapping.category().id());
                    assertThat(found.textPatterns()).containsExactlyInAnyOrderElementsOf(mapping.textPatterns());
                });

        Optional<CategoryMapping> best = repository.findBestMappingForPattern(pattern);
        assertThat(best).contains(mapping);
    }

    @Test
    void saveShouldReplaceExistingPatterns() {
        CategoryMappingId mappingId = CategoryMappingId.generate();
        repository.save(createMapping(
                mappingId,
                Category.ynabCategory("travel", "Travel"),
                0.70,
                2,
                "old pattern"
        ));

        CategoryMapping updated = repository.save(createMapping(
                mappingId,
                Category.ynabCategory("travel", "Travel"),
                0.92,
                4,
                "new pattern"
        ));

        List<CategoryMapping> results = repository.findMappingsForPattern(new TransactionPattern(Set.of("new pattern")));
        assertThat(results).singleElement().satisfies(found -> {
            assertThat(found.id()).isEqualTo(mappingId);
            assertThat(found.textPatterns()).containsExactly("new pattern");
            assertThat(found.confidence()).isEqualTo(updated.confidence());
            assertThat(found.occurrenceCount()).isEqualTo(updated.occurrenceCount());
        });

        List<CategoryMapping> oldPatternResults = repository.findMappingsForPattern(new TransactionPattern(Set.of("old pattern")));
        assertThat(oldPatternResults).isEmpty();
    }

    @Test
    void findMappingsContainingAnyPatternShouldReturnMatches() {
        CategoryMapping first = repository.save(createMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory("utilities", "Utilities"),
                0.80,
                2,
                "utility company", "monthly bill"
        ));

        CategoryMapping second = repository.save(createMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory("rent", "Rent"),
                0.95,
                6,
                "leasing office"
        ));

        List<CategoryMapping> results = repository.findMappingsContainingAnyPattern(List.of("leasing office", "utility company"));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(CategoryMapping::id)
                .containsExactly(second.id(), first.id()); // ordered by confidence desc
    }

    @Test
    void saveAllShouldPersistBatch() {
        CategoryMapping first = createMapping(
                CategoryMappingId.of(UUID.randomUUID().toString()),
                Category.ynabCategory("fun", "Entertainment"),
                0.60,
                1,
                "cinema"
        );
        CategoryMapping second = createMapping(
                CategoryMappingId.of(UUID.randomUUID().toString()),
                Category.ynabCategory("coffee", "Coffee Shops"),
                0.88,
                3,
                "coffee bar"
        );

        repository.saveAll(List.of(first, second));

        List<CategoryMapping> cinemaResults = repository.findMappingsForPattern(new TransactionPattern(Set.of("cinema")));
        List<CategoryMapping> coffeeResults = repository.findMappingsForPattern(new TransactionPattern(Set.of("coffee bar")));

        assertThat(cinemaResults).hasSize(1);
        assertThat(coffeeResults).hasSize(1);
    }

    @Test
    void saveAllShouldRejectEmptyCollection() {
        assertThatThrownBy(() -> repository.saveAll(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    private CategoryMapping createMapping(
            CategoryMappingId id,
            Category category,
            double confidence,
            int occurrenceCount,
            String... patterns
    ) {
        return new CategoryMapping(
                id,
                category,
                Set.of(patterns),
                confidence,
                occurrenceCount
        );
    }
}
