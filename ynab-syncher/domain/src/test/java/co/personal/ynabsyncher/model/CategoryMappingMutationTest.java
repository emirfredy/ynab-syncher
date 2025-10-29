package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation testing focused tests for CategoryMapping boolean methods and boundaries.
 * These tests target specific mutation types that need better coverage.
 */
@DisplayName("CategoryMapping Mutation Tests")
class CategoryMappingMutationTest {

    @Nested
    @DisplayName("isHighConfidence Boundary Testing")
    class IsHighConfidenceBoundaryTesting {

        @Test
        @DisplayName("should return false when confidence is exactly below 0.8 threshold")
        void shouldReturnFalseWhenConfidenceBelowThreshold() {
            // Boundary: 0.8 - epsilon
            CategoryMapping mapping = createCategoryMapping(0.79, 5);
            
            assertThat(mapping.isHighConfidence()).isFalse();
        }

        @Test
        @DisplayName("should return false when confidence is exactly at 0.8 but occurrence count below 2")
        void shouldReturnFalseWhenOccurrenceCountBelowThreshold() {
            // Boundary: occurrence count < 2
            CategoryMapping mapping = createCategoryMapping(0.8, 1);
            
            assertThat(mapping.isHighConfidence()).isFalse();
        }

        @Test
        @DisplayName("should return true when confidence exactly at 0.8 and occurrence count exactly at 2")
        void shouldReturnTrueAtExactBoundary() {
            // Boundary: exactly at threshold values
            CategoryMapping mapping = createCategoryMapping(0.8, 2);
            
            assertThat(mapping.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("should return true when confidence above 0.8 and occurrence count above 2")
        void shouldReturnTrueAboveThreshold() {
            CategoryMapping mapping = createCategoryMapping(0.85, 3);
            
            assertThat(mapping.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("should return false when confidence is 1.0 but occurrence count is 1")
        void shouldReturnFalseWhenHighConfidenceButLowOccurrence() {
            // Test AND condition - both conditions must be true
            CategoryMapping mapping = createCategoryMapping(1.0, 1);
            
            assertThat(mapping.isHighConfidence()).isFalse();
        }

        @Test
        @DisplayName("should return false when occurrence count is high but confidence is low")
        void shouldReturnFalseWhenHighOccurrenceButLowConfidence() {
            // Test AND condition - both conditions must be true
            CategoryMapping mapping = createCategoryMapping(0.5, 10);
            
            assertThat(mapping.isHighConfidence()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasExactMatch Boolean Testing")
    class HasExactMatchBooleanTesting {

        @Test
        @DisplayName("should return true when patterns match exactly")
        void shouldReturnTrueWhenPatternsMatch() {
            CategoryMapping mapping = createCategoryMapping(0.8, 2);
            TransactionPattern matchingPattern = new TransactionPattern(Set.of("coffee", "shop"));
            
            boolean result = mapping.hasExactMatch(matchingPattern);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when patterns do not match")
        void shouldReturnFalseWhenPatternsDoNotMatch() {
            CategoryMapping mapping = createCategoryMapping(0.8, 2);
            TransactionPattern differentPattern = new TransactionPattern(Set.of("grocery", "store"));
            
            boolean result = mapping.hasExactMatch(differentPattern);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when pattern is subset but contains matching text")
        void shouldReturnTrueWhenPatternIsSubsetButContainsMatchingText() {
            CategoryMapping mapping = createCategoryMapping(0.8, 2);
            TransactionPattern subsetPattern = new TransactionPattern(Set.of("coffee")); // Contains "coffee" which is in mapping
            
            boolean result = mapping.hasExactMatch(subsetPattern);
            
            assertThat(result).isTrue(); // Should return true because "coffee" matches
        }

        @Test
        @DisplayName("should return true when pattern is superset but contains matching text")
        void shouldReturnTrueWhenPatternIsSupersetButContainsMatchingText() {
            CategoryMapping mapping = createCategoryMapping(0.8, 2);
            TransactionPattern supersetPattern = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            
            boolean result = mapping.hasExactMatch(supersetPattern);
            
            assertThat(result).isTrue(); // Should return true because "coffee" and "shop" match
        }
    }

    // Helper method
    private CategoryMapping createCategoryMapping(double confidence, int occurrenceCount) {
        return new CategoryMapping(
            CategoryMappingId.generate(),
            Category.ynabCategory("cat-123", "Coffee"),
            Set.of("coffee", "shop"),
            confidence,
            occurrenceCount
        );
    }
}