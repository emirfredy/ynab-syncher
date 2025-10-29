package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation testing focused tests for CategoryInferenceResult boolean methods.
 * These tests target BooleanTrueReturnValsMutator and boundary conditions.
 */
@DisplayName("CategoryInferenceResult Mutation Tests")
class CategoryInferenceResultMutationTest {

    @Nested
    @DisplayName("Boolean Method Testing")
    class BooleanMethodTesting {

        @Test
        @DisplayName("hasMatch should return true when category is not unknown")
        void hasMatchShouldReturnTrueWhenCategoryIsNotUnknown() {
            Category knownCategory = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                knownCategory, 0.8, "Exact pattern match"
            );
            
            assertThat(result.hasMatch()).isTrue();
        }

        @Test
        @DisplayName("hasMatch should return false when category is unknown")
        void hasMatchShouldReturnFalseWhenCategoryIsUnknown() {
            Category unknownCategory = Category.unknown();
            CategoryInferenceResult result = new CategoryInferenceResult(
                unknownCategory, 0.0, "No match found"
            );
            
            assertThat(result.hasMatch()).isFalse();
        }

        @Test
        @DisplayName("hasMatch should return true for inferred category")
        void hasMatchShouldReturnTrueForInferredCategory() {
            Category inferredCategory = Category.inferredCategory("Inferred Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                inferredCategory, 0.5, "Similarity match"
            );
            
            assertThat(result.hasMatch()).isTrue();
        }

        @Test
        @DisplayName("isHighConfidence should return true when confidence exactly at 0.8")
        void isHighConfidenceShouldReturnTrueAtBoundary() {
            Category category = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                category, 0.8, "Boundary test"
            );
            
            assertThat(result.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("isHighConfidence should return false when confidence just below 0.8")
        void isHighConfidenceShouldReturnFalseJustBelowBoundary() {
            Category category = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                category, 0.79, "Just below boundary"
            );
            
            assertThat(result.isHighConfidence()).isFalse();
        }

        @Test
        @DisplayName("isHighConfidence should return true when confidence above 0.8")
        void isHighConfidenceShouldReturnTrueAboveBoundary() {
            Category category = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                category, 0.9, "Above boundary"
            );
            
            assertThat(result.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("isHighConfidence should return true when confidence is 1.0")
        void isHighConfidenceShouldReturnTrueAtMaximum() {
            Category category = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                category, 1.0, "Perfect match"
            );
            
            assertThat(result.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("isHighConfidence should return false when confidence is 0.0")
        void isHighConfidenceShouldReturnFalseAtMinimum() {
            Category category = Category.ynabCategory("cat-123", "Groceries");
            CategoryInferenceResult result = new CategoryInferenceResult(
                category, 0.0, "No confidence"
            );
            
            assertThat(result.isHighConfidence()).isFalse();
        }
    }
}