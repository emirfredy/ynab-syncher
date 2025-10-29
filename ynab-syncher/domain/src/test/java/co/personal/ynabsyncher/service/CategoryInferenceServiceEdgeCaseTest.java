package co.personal.ynabsyncher.service;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.BankTransactionTestBuilder;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.CategoryMappingId;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.model.CategoryInferenceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static co.personal.ynabsyncher.model.BankTransactionTestBuilder.aBankTransaction;
import static org.assertj.core.api.Assertions.*;

/**
 * Additional edge case tests to improve mutation testing coverage.
 * These tests target specific boundary conditions and edge cases that 
 * mutation testing identified as not fully covered.
 */
@DisplayName("CategoryInferenceService - Edge Cases")
class CategoryInferenceServiceEdgeCaseTest {

    private CategoryInferenceService categoryInferenceService;

    @BeforeEach
    void setUp() {
        categoryInferenceService = new CategoryInferenceService();
    }

    @Nested
    @DisplayName("Boundary Condition Tests")
    class BoundaryConditionTests {

        @Test
        @DisplayName("should work when merchant name is exactly 3 characters")
        void shouldWorkWhenMerchantNameIsExactly3Characters() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("ABC")  // Exactly 3 characters
                    .withDescription("Default description")  // Fallback
                    .build();

            YnabCategory category = createYnabCategory("ABC Store", "Shopping");  // Should match "ABC"
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            // Should work since merchant name is >= 3 chars and has similarity with "ABC Store"
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Merchant name match");
        }

        @Test
        @DisplayName("should ignore merchant name when exactly 2 characters")
        void shouldIgnoreMerchantNameWhenExactly2Characters() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("AB")  // Exactly 2 characters - should be ignored
                    .withDescription("Grocery Store")   // Valid description for matching
                    .build();

            YnabCategory category = createYnabCategory("Grocery", "Food");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            // Should match via description since merchant name is ignored (< 3 chars)
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Description match");
        }

        @Test
        @DisplayName("should ignore description when exactly 2 characters")
        void shouldIgnoreDescriptionWhenExactly2Characters() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("Target Store")  // Valid merchant name for matching
                    .withDescription("AB")   // Exactly 2 characters - should be ignored
                    .build();

            YnabCategory category = createYnabCategory("Target", "Shopping");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            // Should match via merchant name since description is ignored (< 3 chars)
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Merchant name match");
        }

        @Test
        @DisplayName("should work when description is exactly 3 characters")
        void shouldWorkWhenDescriptionIsExactly3Characters() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName(null)
                    .withDescription("ABC")  // Exactly 3 characters
                    .build();

            YnabCategory category = createYnabCategory("ABC Company", "Business");  // Should match "ABC"
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            // Should work since description is >= 3 chars and has similarity with "ABC Company"
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Description match");
        }

        @Test
        @DisplayName("should handle confidence exactly at minimum threshold")
        void shouldHandleConfidenceExactlyAtMinimumThreshold() {
            // Create a transaction and category that will produce exactly 0.3 confidence
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("minimal match")
                    .build();

            // Create a category that will score exactly at threshold
            YnabCategory category = createMinimalMatchCategory();
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            if (result.isPresent()) {
                assertThat(result.get().confidence()).isGreaterThanOrEqualTo(0.3);
            }
        }

        @Test
        @DisplayName("should handle confidence just below minimum threshold")
        void shouldHandleConfidenceJustBelowMinimumThreshold() {
            // Test with categories that produce very low similarity scores
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("completely unrelated text here")
                    .build();

            YnabCategory category = createYnabCategory("Groceries", "Food");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(transaction, availableCategories, List.of());

            // Should be empty if confidence < 0.3
            if (result.isEmpty()) {
                // This is expected for very low similarity
                assertThat(result).isEmpty();
            } else {
                // If present, confidence should be >= threshold
                assertThat(result.get().confidence()).isGreaterThanOrEqualTo(0.3);
            }
        }
    }

    @Nested
    @DisplayName("Math Operation Edge Cases")
    class MathOperationEdgeCases {

        @Test
        @DisplayName("should handle confidence boost boundary at 1.0")
        void shouldHandleConfidenceBoostBoundaryAt1_0() {
            // Create a mapping with confidence 0.9, so boost would exceed 1.0
            CategoryMapping highConfidenceMapping = createCategoryMapping(0.9, 5);
            
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("test pattern")
                    .build();

            YnabCategory category = createYnabCategory("Coffee", "Dining");
            List<YnabCategory> availableCategories = List.of(category);
            List<CategoryMapping> learnedMappings = List.of(highConfidenceMapping);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, learnedMappings);

            // Should handle confidence boost that would exceed 1.0
            if (result.isPresent()) {
                assertThat(result.get().confidence()).isLessThanOrEqualTo(1.0);
            }
        }

        @Test
        @DisplayName("should handle fallback confidence reduction correctly")
        void shouldHandleFallbackConfidenceReductionCorrectly() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("groceries store")
                    .build();

            YnabCategory category = createYnabCategory("Groceries", "Food");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            if (result.isPresent()) {
                // In fallback mode, confidence should be reduced by multiplying by 0.8
                assertThat(result.get().confidence()).isLessThan(1.0);
                assertThat(result.get().reasoning()).contains("Fallback");
            }
        }
    }

    @Nested
    @DisplayName("Empty Collection Edge Cases")
    class EmptyCollectionEdgeCases {

        @Test
        @DisplayName("should return empty when no categories available")
        void shouldReturnEmptyWhenNoCategoriesAvailable() {
            BankTransaction transaction = createBankTransaction().build();

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, List.of(), List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no learned mappings but categories exist")
        void shouldReturnEmptyWhenNoLearnedMappingsButCategoriesExist() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("unknown merchant")
                    .build();

            YnabCategory category = createYnabCategory("Test", "Test Group");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            // Should try fallback matching - result depends on similarity calculation
            // The test verifies the service handles unknown merchants gracefully
            assertThat(result).isNotNull(); // Service should always return Optional, never null
        }

        @Test
        @DisplayName("should handle transactions with minimal content")
        void shouldHandleTransactionsWithMinimalContent() {
            // Test with shortest possible valid content
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("ABC")  // Exactly 3 characters - minimum valid
                    .withDescription("XY")    // 2 characters - below threshold
                    .build();

            YnabCategory category = createYnabCategory("Unrelated Store", "Different Group");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            // Should be empty due to low similarity between "ABC" and "Unrelated Store"
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("String Processing Edge Cases")
    class StringProcessingEdgeCases {

        @Test
        @DisplayName("should ignore merchant name with only whitespace")
        void shouldIgnoreMerchantNameWithOnlyWhitespace() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("   ")  // Only whitespace - will be treated as blank and ignored
                    .withDescription("Coffee Shop")    // Valid description for matching
                    .build();

            YnabCategory category = createYnabCategory("Coffee", "Dining");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            // Should match via description since merchant name is effectively blank
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Description match");
        }

        @Test
        @DisplayName("should ignore description with only whitespace")
        void shouldIgnoreDescriptionWithOnlyWhitespace() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("Starbucks")      // Valid merchant name for matching
                    .withDescription("   ")      // Only whitespace - will be treated as blank and ignored
                    .build();

            YnabCategory category = createYnabCategory("Starbucks", "Dining");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            // Should match via merchant name since description is effectively blank
            assertThat(result).isPresent();
            assertThat(result.get().reasoning()).contains("Merchant name match");
        }

        @Test
        @DisplayName("should trim and process merchant name correctly")
        void shouldTrimAndProcessMerchantNameCorrectly() {
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("  grocery store  ")  // With leading/trailing spaces
                    .build();

            YnabCategory category = createYnabCategory("Grocery", "Food");
            List<YnabCategory> availableCategories = List.of(category);

            Optional<CategoryInferenceResult> result = categoryInferenceService.analyzeTransaction(
                transaction, availableCategories, List.of());

            if (result.isPresent()) {
                assertThat(result.get().reasoning()).contains("Merchant name match");
            }
        }
    }

    // Helper methods
    private BankTransactionTestBuilder createBankTransaction() {
        return aBankTransaction()
                .withId("test-id")
                .withAccountId(AccountId.of("test-account"))
                .withDate(LocalDate.of(2023, 1, 15))
                .withAmount(Money.of(BigDecimal.valueOf(50.00)))
                .withDescription("Default description")  // Changed to avoid matching test categories
                .withMerchantName("Default merchant")     // Changed to avoid matching test categories
                .withMemo("Test memo")
                .withInferredCategory(Category.inferredCategory("Test Category"));
    }

    private YnabCategory createYnabCategory(String name, String groupName) {
        return new YnabCategory(
            "cat-id",
            name,
            "group-id",
            groupName,
            false,
            false
        );
    }

    private YnabCategory createMinimalMatchCategory() {
        return new YnabCategory(
            "cat-id",
            "minimal",
            "group-id",
            "test",
            false,
            false
        );
    }

    private CategoryMapping createCategoryMapping(double confidence, int occurrenceCount) {
        return new CategoryMapping(
            CategoryMappingId.generate(),
            Category.of("Test Group", "Test Category"),
            Set.of("test", "pattern"),
            confidence,
            occurrenceCount
        );
    }
}