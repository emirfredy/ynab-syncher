package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.model.CategoryInferenceResult;
import co.personal.ynabsyncher.service.CategoryInferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CategoryInferenceService")
class CategoryInferenceServiceTest {

    private CategoryInferenceService categoryInferenceService;

    @BeforeEach
    void setUp() {
        categoryInferenceService = new CategoryInferenceService();
    }

    @Nested
    @DisplayName("Merchant Name Matching")
    class MerchantNameMatching {

        @Test
        @DisplayName("should match category by merchant name with exact match")
        void shouldMatchCategoryByMerchantNameWithExactMatch() {
            // Given
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("KROGER GROCERIES STORE")  // Changed to match category name
                    .build();

            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(groceriesCategory);

            // When
            Optional<CategoryInferenceResult> resultOptional = categoryInferenceService.analyzeTransaction(transaction, availableCategories);

            // Then
            assertThat(resultOptional).isPresent();
            CategoryInferenceResult result = resultOptional.get();
            assertThat(result.hasMatch()).isTrue();
            assertThat(result.category().name()).isEqualTo("Food & Dining: Groceries");
            assertThat(result.confidence()).isEqualTo(1.0);
            assertThat(result.reasoning()).contains("Merchant name match");
            assertThat(result.isHighConfidence()).isTrue();
        }        @Test
        @DisplayName("should not match when merchant name is null")
        void shouldNotMatchWhenMerchantNameIsNull() {
            // Given
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName(null)
                    .build();

            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(groceriesCategory);

            // When
            Optional<CategoryInferenceResult> resultOptional = categoryInferenceService.analyzeTransaction(transaction, availableCategories);

            // Then
            assertThat(resultOptional).isEmpty();
        }
    }

    @Nested
    @DisplayName("Description-Based Matching")
    class DescriptionBasedMatching {

        @Test
        @DisplayName("should fall back to description matching when merchant match fails")
        void shouldFallBackToDescriptionMatchingWhenMerchantMatchFails() {
            // Given
            BankTransaction transaction = createBankTransaction()
                    .withMerchantName("UNKNOWN MERCHANT")
                    .withDescription("Payment to GROCERIES OUTLET")  // Changed to match category name
                    .build();

            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(groceriesCategory);

            // When
            Optional<CategoryInferenceResult> resultOptional = categoryInferenceService.analyzeTransaction(transaction, availableCategories);

            // Then
            assertThat(resultOptional).isPresent();
            CategoryInferenceResult result = resultOptional.get();
            assertThat(result.hasMatch()).isTrue();
            assertThat(result.category().name()).isEqualTo("Food & Dining: Groceries");
            assertThat(result.reasoning()).contains("Description match");
        }
    }

    @Nested
    @DisplayName("CategoryInferenceResult")
    class CategoryInferenceResultTests {

        @Test
        @DisplayName("should validate confidence range")
        void shouldValidateConfidenceRange() {
            // Given
            Category category = Category.ynabCategory("cat_1", "Test");

            // When & Then
            assertThatThrownBy(() -> new CategoryInferenceResult(category, -0.1, "Invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confidence must be between 0.0 and 1.0");

            assertThatThrownBy(() -> new CategoryInferenceResult(category, 1.1, "Invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confidence must be between 0.0 and 1.0");
        }

        @Test
        @DisplayName("should create no match result")
        void shouldCreateNoMatchResult() {
            // When
            CategoryInferenceResult result = CategoryInferenceResult.noMatch();

            // Then
            assertThat(result.hasMatch()).isFalse();
            assertThat(result.confidence()).isEqualTo(0.0);
            assertThat(result.category()).isEqualTo(Category.unknown());
            assertThat(result.reasoning()).contains("No suitable match found");
        }

        @Test
        @DisplayName("should identify high confidence results")
        void shouldIdentifyHighConfidenceResults() {
            // Given
            Category category = Category.ynabCategory("cat_1", "Test");

            // When
            CategoryInferenceResult highConfidence = new CategoryInferenceResult(category, 0.9, "High");
            CategoryInferenceResult lowConfidence = new CategoryInferenceResult(category, 0.5, "Low");

            // Then
            assertThat(highConfidence.isHighConfidence()).isTrue();
            assertThat(lowConfidence.isHighConfidence()).isFalse();
        }
    }

    // Helper methods
    private BankTransactionBuilder createBankTransaction() {
        return new BankTransactionBuilder();
    }

    private YnabCategory createYnabCategory(String name, String groupName) {
        return new YnabCategory("cat_" + name.hashCode(), name, "grp_" + groupName.hashCode(), groupName, false, false);
    }

    private static class BankTransactionBuilder {
        private String merchantName = "DEFAULT MERCHANT";
        private String description = "DEFAULT DESCRIPTION";
        private String memo = "";
        private Money amount = Money.of(new BigDecimal("-50.00"));

        public BankTransactionBuilder withMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public BankTransactionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public BankTransactionBuilder withMemo(String memo) {
            this.memo = memo;
            return this;
        }

        public BankTransactionBuilder withAmount(Money amount) {
            this.amount = amount;
            return this;
        }

        public BankTransaction build() {
            return new BankTransaction(
                    TransactionId.of("txn_" + System.currentTimeMillis()),
                    AccountId.of("account_123"),
                    LocalDate.now(),
                    amount,
                    description,
                    merchantName,
                    memo,
                    "DEBIT",
                    "REF123",
                    Category.unknown()
            );
        }
    }
}