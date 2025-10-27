package co.personal.ynabsyncher.model.ynab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("YnabCategory")
class YnabCategoryTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create valid category with all required fields")
        void shouldCreateValidCategory() {
            // Given
            String id = "cat_123";
            String name = "Groceries";
            String groupId = "group_456";
            String groupName = "Food & Dining";
            boolean isHidden = false;
            boolean isDeleted = false;

            // When
            YnabCategory category = new YnabCategory(id, name, groupId, groupName, isHidden, isDeleted);

            // Then
            assertThat(category.id()).isEqualTo(id);
            assertThat(category.name()).isEqualTo(name);
            assertThat(category.groupId()).isEqualTo(groupId);
            assertThat(category.groupName()).isEqualTo(groupName);
            assertThat(category.isHidden()).isEqualTo(isHidden);
            assertThat(category.isDeleted()).isEqualTo(isDeleted);
        }

        @Test
        @DisplayName("should reject null ID")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> new YnabCategory(null, "Groceries", "group_1", "Food", false, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Category ID cannot be null");
        }

        @Test
        @DisplayName("should reject blank ID")
        void shouldRejectBlankId() {
            assertThatThrownBy(() -> new YnabCategory("", "Groceries", "group_1", "Food", false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category ID cannot be blank");
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> new YnabCategory("cat_1", null, "group_1", "Food", false, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Category name cannot be null");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new YnabCategory("cat_1", "  ", "group_1", "Food", false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category name cannot be blank");
        }
    }

    @Nested
    @DisplayName("Availability for Inference")
    class AvailabilityForInference {

        @Test
        @DisplayName("should be available when not hidden and not deleted")
        void shouldBeAvailableWhenNotHiddenAndNotDeleted() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When & Then
            assertThat(category.isAvailableForInference()).isTrue();
        }

        @Test
        @DisplayName("should not be available when hidden")
        void shouldNotBeAvailableWhenHidden() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", true, false);

            // When & Then
            assertThat(category.isAvailableForInference()).isFalse();
        }

        @Test
        @DisplayName("should not be available when deleted")
        void shouldNotBeAvailableWhenDeleted() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, true);

            // When & Then
            assertThat(category.isAvailableForInference()).isFalse();
        }

        @Test
        @DisplayName("should not be available when both hidden and deleted")
        void shouldNotBeAvailableWhenBothHiddenAndDeleted() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", true, true);

            // When & Then
            assertThat(category.isAvailableForInference()).isFalse();
        }
    }

    @Nested
    @DisplayName("Full Name Generation")
    class FullNameGeneration {

        @Test
        @DisplayName("should generate full name with group and category")
        void shouldGenerateFullNameWithGroupAndCategory() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food & Dining", false, false);

            // When
            String fullName = category.getFullName();

            // Then
            assertThat(fullName).isEqualTo("Food & Dining: Groceries");
        }

        @Test
        @DisplayName("should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Auto & Transport", "group_1", "Transportation", false, false);

            // When
            String fullName = category.getFullName();

            // Then
            assertThat(fullName).isEqualTo("Transportation: Auto & Transport");
        }
    }

    @Nested
    @DisplayName("Inference Keywords")
    class InferenceKeywords {

        @Test
        @DisplayName("should generate keywords for category and group")
        void shouldGenerateKeywordsForCategoryAndGroup() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food & Dining", false, false);

            // When
            String[] keywords = category.getInferenceKeywords();

            // Then
            assertThat(keywords).containsExactly(
                    "groceries",
                    "food & dining",
                    "food & dining: groceries"
            );
        }

        @Test
        @DisplayName("should convert keywords to lowercase")
        void shouldConvertKeywordsToLowercase() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Auto Insurance", "group_1", "Transportation", false, false);

            // When
            String[] keywords = category.getInferenceKeywords();

            // Then
            assertThat(keywords).allMatch(keyword -> keyword.equals(keyword.toLowerCase()));
        }
    }

    @Nested
    @DisplayName("Similarity Score Calculation")
    class SimilarityScoreCalculation {

        @Test
        @DisplayName("should return 1.0 for exact category name match")
        void shouldReturn1ForExactCategoryNameMatch() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When
            double score = category.calculateSimilarityScore("Payment to GROCERIES STORE");

            // Then
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should return 0.7 for group name match")
        void shouldReturn0Point7ForGroupNameMatch() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Restaurants", "group_1", "Food", false, false);

            // When
            double score = category.calculateSimilarityScore("Payment to FOOD MART");

            // Then
            assertThat(score).isEqualTo(0.7);
        }

        @Test
        @DisplayName("should return 0.5 for partial word match")
        void shouldReturn0Point5ForPartialWordMatch() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Auto Insurance", "group_1", "Transportation", false, false);

            // When
            double score = category.calculateSimilarityScore("Payment to STATE FARM AUTO");

            // Then
            assertThat(score).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should return 0.0 for no match")
        void shouldReturn0ForNoMatch() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When
            double score = category.calculateSimilarityScore("Payment to GAS STATION");

            // Then
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 for null text")
        void shouldReturn0ForNullText() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When
            double score = category.calculateSimilarityScore(null);

            // Then
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 for blank text")
        void shouldReturn0ForBlankText() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When
            double score = category.calculateSimilarityScore("   ");

            // Then
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Groceries", "group_1", "Food", false, false);

            // When
            double score1 = category.calculateSimilarityScore("GROCERIES STORE");
            double score2 = category.calculateSimilarityScore("groceries store");
            double score3 = category.calculateSimilarityScore("Groceries Store");

            // Then
            assertThat(score1).isEqualTo(1.0);
            assertThat(score2).isEqualTo(1.0);
            assertThat(score3).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should skip short words in partial matching")
        void shouldSkipShortWordsInPartialMatching() {
            // Given
            YnabCategory category = new YnabCategory("cat_1", "Gas & Auto", "group_1", "Transportation", false, false);

            // When
            double score = category.calculateSimilarityScore("Payment to GAS station");

            // Then
            // "Gas" should match (>=3 chars), "&" should be skipped (<3 chars)
            assertThat(score).isEqualTo(0.5);
        }
    }
}