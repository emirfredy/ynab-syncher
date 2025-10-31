package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.CategoryMappingId;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Behavioral contract that every {@link CategoryMappingRepository} implementation should satisfy.
 * The scenarios mirror the expectations enforced by domain services around exact-pattern matching
 * and result ordering. Implementations extend this class and supply repository access plus a
 * cleanup callback between tests.
 */
abstract class CategoryMappingRepositoryContractTest {

    protected abstract CategoryMappingRepository repository();

    protected abstract void clearRepository();

    @BeforeEach
    void resetRepositoryState() {
        clearRepository();
    }

    @Test
    @DisplayName("findMappingsForPattern should match mappings containing any queried pattern")
    void findMappingsForPatternShouldMatchAnyPattern() {
        CategoryMapping matching = saveMapping("cat-1", 0.7, 3, "coffee shop", "morning coffee");
        saveMapping("cat-2", 0.5, 1, "grocery store");

        TransactionPattern queryPattern = new TransactionPattern(Set.of("morning coffee", "brunch"));

        List<CategoryMapping> results = repository().findMappingsForPattern(queryPattern);

        assertThat(results)
                .extracting(CategoryMapping::id)
                .containsExactly(matching.id());
    }

    @Test
    @DisplayName("findMappingsForPattern should deduplicate results even when multiple patterns match")
    void findMappingsForPatternShouldDeduplicateResults() {
        CategoryMapping mapping = saveMapping("cat-unique", 0.9, 2, "monthly rent", "rent payment");

        TransactionPattern pattern = new TransactionPattern(Set.of("monthly rent", "rent payment"));

        List<CategoryMapping> results = repository().findMappingsForPattern(pattern);

        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(CategoryMapping::id)
                .isEqualTo(mapping.id());
    }

    @Test
    @DisplayName("findMappingsForPattern should order by confidence then occurrence count")
    void findMappingsForPatternOrderingIsStable() {
        saveMapping("cat-low", 0.60, 4, "restaurant");
        saveMapping("cat-mid", 0.85, 2, "restaurant");
        saveMapping("cat-high-a", 0.90, 5, "restaurant");
        saveMapping("cat-high-b", 0.90, 3, "restaurant");

        List<CategoryMapping> ordered = repository().findMappingsForPattern(new TransactionPattern(Set.of("restaurant")));

        assertThat(ordered)
                .extracting(CategoryMapping::confidence, CategoryMapping::occurrenceCount)
                .containsExactly(
                        tuple(0.90, 5),
                        tuple(0.90, 3),
                        tuple(0.85, 2),
                        tuple(0.60, 4)
                );
    }

    @Test
    @DisplayName("findMappingsContainingAnyPattern should match any of the provided patterns")
    void findMappingsContainingAnyPatternShouldMatchSuppliedValues() {
        CategoryMapping groceries = saveMapping("cat-grocery", 0.78, 4, "fresh market", "weekly grocery");
        CategoryMapping utilities = saveMapping("cat-utilities", 0.65, 2, "power company");

        List<CategoryMapping> results = repository().findMappingsContainingAnyPattern(List.of("weekly grocery", "unknown"));

        assertThat(results)
                .extracting(CategoryMapping::id)
                .containsExactly(groceries.id());

        List<CategoryMapping> combined = repository().findMappingsContainingAnyPattern(List.of("power company", "fresh market"));
        assertThat(combined)
                .extracting(CategoryMapping::id)
                .containsExactly(groceries.id(), utilities.id());
    }

    @Test
    @DisplayName("findMappingsForCategory should sort by confidence for a given category")
    void findMappingsForCategoryShouldSortByConfidence() {
        Category category = Category.ynabCategory("shared", "Shared");
        CategoryMapping high = repository().save(new CategoryMapping(
                CategoryMappingId.generate(),
                category,
                Set.of("pattern-one"),
                0.92,
                5
        ));

        CategoryMapping low = repository().save(new CategoryMapping(
                CategoryMappingId.generate(),
                category,
                Set.of("pattern-two"),
                0.60,
                1
        ));

        List<CategoryMapping> results = repository().findMappingsForCategory(category);

        assertThat(results)
                .extracting(CategoryMapping::id)
                .containsExactly(high.id(), low.id());
    }

    private CategoryMapping saveMapping(String categoryId, double confidence, int occurrenceCount, String... patterns) {
        CategoryMapping mapping = new CategoryMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory(categoryId, categoryId + " Name"),
                Set.of(patterns),
                confidence,
                occurrenceCount
        );
        return repository().save(mapping);
    }
}

