package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse.TransactionCategoryResult;
import co.personal.ynabsyncher.api.usecase.InferTransactionCategories;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import co.personal.ynabsyncher.service.CategoryInferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static co.personal.ynabsyncher.model.BankTransactionTestBuilder.aBankTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

@DisplayName("Infer Transaction Categories Use Case - Functional Tests")
class InferTransactionCategoriesUseCaseFunctionalTest {

    @Mock
    private BankTransactionRepository bankTransactionRepository;
    
    @Mock
    private YnabCategoryRepository ynabCategoryRepository;
    
    @Mock
    private CategoryInferenceService categoryInferenceService;
    
    private InferTransactionCategories inferTransactionCategories;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inferTransactionCategories = new InferTransactionCategoriesUseCase(
            bankTransactionRepository, 
            ynabCategoryRepository,
            categoryInferenceService
        );
    }

    @Nested
    @DisplayName("Single Transaction Inference")
    class SingleTransactionInference {

        @Test
        @DisplayName("Should successfully infer category for single uncategorized transaction")
        void shouldSuccessfullyInferCategoryForSingleUncategorizedTransaction() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction uncategorizedTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("Grocery Store")
                .withDescription("Weekly shopping")
                .withAmount(Money.of(125.50))
                .withInferredCategory(Category.unknown())
                .build();
            
            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(groceriesCategory);
            
            CategoryInferenceResult domainResult = new CategoryInferenceResult(
                Category.ynabCategory("groceries", "Groceries"), 
                0.85, 
                "Matched by merchant name pattern"
            );
            
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(uncategorizedTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories)))
                .thenReturn(Optional.of(domainResult));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);
            assertThat(response.failedInferences()).isEqualTo(0);
            assertThat(response.results()).hasSize(1);
            
            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.inferenceResult().category().name()).isEqualTo("Groceries");
            assertThat(result.inferenceResult().confidence()).isEqualTo(0.85);
            assertThat(result.inferenceResult().reasoning()).isEqualTo("Matched by merchant name pattern");
            
            verify(categoryInferenceService).analyzeTransaction(uncategorizedTransaction, availableCategories);
        }

        @Test
        @DisplayName("Should handle single transaction when category inference fails")
        void shouldHandleSingleTransactionWhenCategoryInferenceFails() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction uncategorizedTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("Unknown Merchant XYZ")
                .withDescription("Unclear transaction")
                .withAmount(Money.of(25.00))
                .withInferredCategory(Category.unknown())
                .build();
            
            YnabCategory category = createYnabCategory("Miscellaneous", "General");
            List<YnabCategory> availableCategories = List.of(category);
            
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(uncategorizedTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories)))
                .thenReturn(Optional.empty());
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(0);
            assertThat(response.failedInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            
            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isFalse();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.inferenceResult().hasMatch()).isFalse();
            
            verify(categoryInferenceService).analyzeTransaction(uncategorizedTransaction, availableCategories);
        }

        @Test
        @DisplayName("Should return existing category for already categorized transaction")
        void shouldReturnExistingCategoryForAlreadyCategorizedTransaction() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            Category existingCategory = Category.ynabCategory("dining", "Dining Out");
            BankTransaction categorizedTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("Restaurant ABC")
                .withDescription("Lunch meeting")
                .withAmount(Money.of(45.00))
                .withInferredCategory(existingCategory)
                .build();
            
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(categorizedTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(List.of(createYnabCategory("Dining Out", "Food & Dining")));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);
            assertThat(response.failedInferences()).isEqualTo(0);
            assertThat(response.results()).hasSize(1);
            
            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.inferenceResult().category()).isEqualTo(existingCategory);
            assertThat(result.inferenceResult().confidence()).isEqualTo(1.0);
            assertThat(result.inferenceResult().reasoning()).isEqualTo("Previously inferred");
            
            // Verify inference service is not called for already categorized transactions
            verifyNoInteractions(categoryInferenceService);
        }
    }

    @Nested
    @DisplayName("Batch Transaction Inference")
    class BatchTransactionInference {

        @Test
        @DisplayName("Should handle batch of mixed categorized and uncategorized transactions")
        void shouldHandleBatchOfMixedCategorizedAndUncategorizedTransactions() {
            // Given
            TransactionId tx1Id = TransactionId.of("tx-1");
            TransactionId tx2Id = TransactionId.of("tx-2");
            TransactionId tx3Id = TransactionId.of("tx-3");
            CategoryInferenceRequest request = new CategoryInferenceRequest(List.of(tx1Id, tx2Id, tx3Id));
            
            // Transaction 1: Already categorized
            Category existingCategory = Category.ynabCategory("gas", "Transportation");
            BankTransaction categorizedTransaction = aBankTransaction()
                .withId(tx1Id)
                .withMerchantName("Gas Station")
                .withInferredCategory(existingCategory)
                .build();
            
            // Transaction 2: Uncategorized, successful inference
            BankTransaction uncategorizedTransaction1 = aBankTransaction()
                .withId(tx2Id)
                .withMerchantName("Coffee Shop")
                .withInferredCategory(Category.unknown())
                .build();
            
            // Transaction 3: Uncategorized, failed inference
            BankTransaction uncategorizedTransaction2 = aBankTransaction()
                .withId(tx3Id)
                .withMerchantName("Unknown Merchant")
                .withInferredCategory(Category.unknown())
                .build();
            
            List<BankTransaction> transactions = List.of(
                categorizedTransaction, 
                uncategorizedTransaction1, 
                uncategorizedTransaction2
            );
            
            YnabCategory diningCategory = createYnabCategory("Dining Out", "Food & Dining");
            YnabCategory transportationCategory = createYnabCategory("Transportation", "Transport");
            List<YnabCategory> availableCategories = List.of(diningCategory, transportationCategory);
            
            CategoryInferenceResult successfulResult = new CategoryInferenceResult(
                Category.ynabCategory("dining", "Dining Out"), 
                0.75, 
                "Matched by business type"
            );
            
            when(bankTransactionRepository.findByIds(List.of(tx1Id, tx2Id, tx3Id)))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction1), eq(availableCategories)))
                .thenReturn(Optional.of(successfulResult));
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction2), eq(availableCategories)))
                .thenReturn(Optional.empty());
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(3);
            assertThat(response.successfulInferences()).isEqualTo(2);
            assertThat(response.failedInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(3);
            
            // Verify already categorized transaction result
            TransactionCategoryResult result1 = findResultById(response.results(), tx1Id);
            assertThat(result1.successful()).isTrue();
            assertThat(result1.inferenceResult().category()).isEqualTo(existingCategory);
            assertThat(result1.inferenceResult().confidence()).isEqualTo(1.0);
            assertThat(result1.inferenceResult().reasoning()).isEqualTo("Previously inferred");
            
            // Verify successful inference result
            TransactionCategoryResult result2 = findResultById(response.results(), tx2Id);
            assertThat(result2.successful()).isTrue();
            assertThat(result2.inferenceResult().category().name()).isEqualTo("Dining Out");
            assertThat(result2.inferenceResult().confidence()).isEqualTo(0.75);
            assertThat(result2.inferenceResult().reasoning()).isEqualTo("Matched by business type");
            
            // Verify failed inference result
            TransactionCategoryResult result3 = findResultById(response.results(), tx3Id);
            assertThat(result3.successful()).isFalse();
            assertThat(result3.inferenceResult().hasMatch()).isFalse();
            
            // Verify service interactions
            verify(categoryInferenceService).analyzeTransaction(uncategorizedTransaction1, availableCategories);
            verify(categoryInferenceService).analyzeTransaction(uncategorizedTransaction2, availableCategories);
            verify(categoryInferenceService, times(2)).analyzeTransaction(any(), any());
        }

        @Test
        @DisplayName("Should handle batch where all transactions are already categorized")
        void shouldHandleBatchWhereAllTransactionsAreAlreadyCategorized() {
            // Given
            TransactionId tx1Id = TransactionId.of("tx-1");
            TransactionId tx2Id = TransactionId.of("tx-2");
            CategoryInferenceRequest request = new CategoryInferenceRequest(List.of(tx1Id, tx2Id));
            
            Category category1 = Category.ynabCategory("groceries", "Groceries");
            Category category2 = Category.ynabCategory("utilities", "Utilities");
            
            BankTransaction categorizedTransaction1 = aBankTransaction()
                .withId(tx1Id)
                .withMerchantName("Grocery Store")
                .withInferredCategory(category1)
                .build();
            
            BankTransaction categorizedTransaction2 = aBankTransaction()
                .withId(tx2Id)
                .withMerchantName("Electric Company")
                .withInferredCategory(category2)
                .build();
            
            List<BankTransaction> transactions = List.of(categorizedTransaction1, categorizedTransaction2);
            
            when(bankTransactionRepository.findByIds(List.of(tx1Id, tx2Id)))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(List.of(createYnabCategory("Miscellaneous", "General")));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(2);
            assertThat(response.successfulInferences()).isEqualTo(2);
            assertThat(response.failedInferences()).isEqualTo(0);
            assertThat(response.results()).hasSize(2);
            
            // All results should be successful with existing categories
            response.results().forEach(result -> {
                assertThat(result.successful()).isTrue();
                assertThat(result.inferenceResult().confidence()).isEqualTo(1.0);
                assertThat(result.inferenceResult().reasoning()).isEqualTo("Previously inferred");
            });
            
            // Verify no inference service calls since all transactions are categorized
            verifyNoInteractions(categoryInferenceService);
        }

        @Test
        @DisplayName("Should handle batch where all transactions need inference")
        void shouldHandleBatchWhereAllTransactionsNeedInference() {
            // Given
            TransactionId tx1Id = TransactionId.of("tx-1");
            TransactionId tx2Id = TransactionId.of("tx-2");
            TransactionId tx3Id = TransactionId.of("tx-3");
            CategoryInferenceRequest request = new CategoryInferenceRequest(List.of(tx1Id, tx2Id, tx3Id));
            
            BankTransaction transaction1 = aBankTransaction()
                .withId(tx1Id)
                .withMerchantName("Coffee Shop")
                .withInferredCategory(Category.unknown())
                .build();
            
            BankTransaction transaction2 = aBankTransaction()
                .withId(tx2Id)
                .withMerchantName("Grocery Store")
                .withInferredCategory(Category.unknown())
                .build();
            
            BankTransaction transaction3 = aBankTransaction()
                .withId(tx3Id)
                .withMerchantName("Gas Station")
                .withInferredCategory(Category.unknown())
                .build();
            
            List<BankTransaction> transactions = List.of(transaction1, transaction2, transaction3);
            
            YnabCategory diningCategory = createYnabCategory("Dining Out", "Food & Dining");
            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            YnabCategory transportationCategory = createYnabCategory("Transportation", "Transport");
            List<YnabCategory> availableCategories = List.of(diningCategory, groceriesCategory, transportationCategory);
            
            CategoryInferenceResult result1 = new CategoryInferenceResult(
                Category.ynabCategory("dining", "Dining Out"), 
                0.90, 
                "Coffee shop pattern match"
            );
            
            CategoryInferenceResult result2 = new CategoryInferenceResult(
                Category.ynabCategory("groceries", "Groceries"), 
                0.95, 
                "Grocery store pattern match"
            );
            
            CategoryInferenceResult result3 = new CategoryInferenceResult(
                Category.ynabCategory("transport", "Transportation"), 
                0.85, 
                "Gas station pattern match"
            );
            
            when(bankTransactionRepository.findByIds(List.of(tx1Id, tx2Id, tx3Id)))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(transaction1), eq(availableCategories)))
                .thenReturn(Optional.of(result1));
            when(categoryInferenceService.analyzeTransaction(eq(transaction2), eq(availableCategories)))
                .thenReturn(Optional.of(result2));
            when(categoryInferenceService.analyzeTransaction(eq(transaction3), eq(availableCategories)))
                .thenReturn(Optional.of(result3));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(3);
            assertThat(response.successfulInferences()).isEqualTo(3);
            assertThat(response.failedInferences()).isEqualTo(0);
            assertThat(response.results()).hasSize(3);
            
            // Verify all transactions got successfully inferred
            TransactionCategoryResult coffeeResult = findResultById(response.results(), tx1Id);
            assertThat(coffeeResult.successful()).isTrue();
            assertThat(coffeeResult.inferenceResult().category().name()).isEqualTo("Dining Out");
            assertThat(coffeeResult.inferenceResult().confidence()).isEqualTo(0.90);
            
            TransactionCategoryResult groceryResult = findResultById(response.results(), tx2Id);
            assertThat(groceryResult.successful()).isTrue();
            assertThat(groceryResult.inferenceResult().category().name()).isEqualTo("Groceries");
            assertThat(groceryResult.inferenceResult().confidence()).isEqualTo(0.95);
            
            TransactionCategoryResult gasResult = findResultById(response.results(), tx3Id);
            assertThat(gasResult.successful()).isTrue();
            assertThat(gasResult.inferenceResult().category().name()).isEqualTo("Transportation");
            assertThat(gasResult.inferenceResult().confidence()).isEqualTo(0.85);
            
            // Verify all transactions were processed by inference service
            verify(categoryInferenceService).analyzeTransaction(transaction1, availableCategories);
            verify(categoryInferenceService).analyzeTransaction(transaction2, availableCategories);
            verify(categoryInferenceService).analyzeTransaction(transaction3, availableCategories);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle empty available categories gracefully")
        void shouldHandleEmptyAvailableCategoriesGracefully() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction uncategorizedTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("Some Merchant")
                .withInferredCategory(Category.unknown())
                .build();
            
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(uncategorizedTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(List.of()); // Empty categories
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(List.of())))
                .thenReturn(Optional.empty());
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(0);
            assertThat(response.failedInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).successful()).isFalse();
            
            verify(categoryInferenceService).analyzeTransaction(uncategorizedTransaction, List.of());
        }

        @Test
        @DisplayName("Should handle inference service returning result without match")
        void shouldHandleInferenceServiceReturningResultWithoutMatch() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction uncategorizedTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("Ambiguous Merchant")
                .withInferredCategory(Category.unknown())
                .build();
            
            YnabCategory category = createYnabCategory("Miscellaneous", "General");
            List<YnabCategory> availableCategories = List.of(category);
            
            // Service returns a result but with no match (low confidence or no category)
            CategoryInferenceResult resultWithoutMatch = new CategoryInferenceResult(
                Category.unknown(), 
                0.0, 
                "No suitable category found"
            );
            
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(uncategorizedTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories)))
                .thenReturn(Optional.of(resultWithoutMatch));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(0);
            assertThat(response.failedInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            
            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isFalse();
            assertThat(result.inferenceResult().hasMatch()).isFalse();
        }

        @Test
        @DisplayName("Should handle complex scenario with various confidence levels")
        void shouldHandleComplexScenarioWithVariousConfidenceLevels() {
            // Given
            TransactionId highConfidenceId = TransactionId.of("tx-high");
            TransactionId mediumConfidenceId = TransactionId.of("tx-medium");
            TransactionId lowConfidenceId = TransactionId.of("tx-low");
            CategoryInferenceRequest request = new CategoryInferenceRequest(
                List.of(highConfidenceId, mediumConfidenceId, lowConfidenceId)
            );
            
            BankTransaction highConfidenceTransaction = aBankTransaction()
                .withId(highConfidenceId)
                .withMerchantName("WALMART GROCERY")
                .withDescription("Grocery shopping")
                .withInferredCategory(Category.unknown())
                .build();
            
            BankTransaction mediumConfidenceTransaction = aBankTransaction()
                .withId(mediumConfidenceId)
                .withMerchantName("CORNER SHOP")
                .withDescription("Purchase")
                .withInferredCategory(Category.unknown())
                .build();
            
            BankTransaction lowConfidenceTransaction = aBankTransaction()
                .withId(lowConfidenceId)
                .withMerchantName("ACME CORP")
                .withDescription("Payment")
                .withInferredCategory(Category.unknown())
                .build();
            
            List<BankTransaction> transactions = List.of(
                highConfidenceTransaction, 
                mediumConfidenceTransaction, 
                lowConfidenceTransaction
            );
            
            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            YnabCategory miscCategory = createYnabCategory("Miscellaneous", "General");
            List<YnabCategory> availableCategories = List.of(groceriesCategory, miscCategory);
            
            CategoryInferenceResult highConfidenceResult = new CategoryInferenceResult(
                Category.ynabCategory("groceries", "Groceries"), 
                0.95, 
                "Strong match: grocery store pattern"
            );
            
            CategoryInferenceResult mediumConfidenceResult = new CategoryInferenceResult(
                Category.ynabCategory("misc", "Miscellaneous"), 
                0.60, 
                "Weak match: general retail pattern"
            );
            
            // Low confidence - no match
            CategoryInferenceResult lowConfidenceResult = new CategoryInferenceResult(
                Category.unknown(), 
                0.25, 
                "No reliable category match found"
            );
            
            when(bankTransactionRepository.findByIds(
                List.of(highConfidenceId, mediumConfidenceId, lowConfidenceId)))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryInferenceService.analyzeTransaction(eq(highConfidenceTransaction), eq(availableCategories)))
                .thenReturn(Optional.of(highConfidenceResult));
            when(categoryInferenceService.analyzeTransaction(eq(mediumConfidenceTransaction), eq(availableCategories)))
                .thenReturn(Optional.of(mediumConfidenceResult));
            when(categoryInferenceService.analyzeTransaction(eq(lowConfidenceTransaction), eq(availableCategories)))
                .thenReturn(Optional.of(lowConfidenceResult));
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(3);
            assertThat(response.successfulInferences()).isEqualTo(2); // High and medium confidence
            assertThat(response.failedInferences()).isEqualTo(1);     // Low confidence
            assertThat(response.results()).hasSize(3);
            
            // Verify high confidence result
            TransactionCategoryResult highResult = findResultById(response.results(), highConfidenceId);
            assertThat(highResult.successful()).isTrue();
            assertThat(highResult.inferenceResult().confidence()).isEqualTo(0.95);
            assertThat(highResult.inferenceResult().category().name()).isEqualTo("Groceries");
            
            // Verify medium confidence result
            TransactionCategoryResult mediumResult = findResultById(response.results(), mediumConfidenceId);
            assertThat(mediumResult.successful()).isTrue();
            assertThat(mediumResult.inferenceResult().confidence()).isEqualTo(0.60);
            assertThat(mediumResult.inferenceResult().category().name()).isEqualTo("Miscellaneous");
            
            // Verify low confidence result (failed)
            TransactionCategoryResult lowResult = findResultById(response.results(), lowConfidenceId);
            assertThat(lowResult.successful()).isFalse();
            assertThat(lowResult.inferenceResult().hasMatch()).isFalse();
        }
    }

    // Helper methods

    private YnabCategory createYnabCategory(String name, String groupName) {
        return new YnabCategory(
            "cat_" + name.hashCode(), 
            name, 
            "grp_" + groupName.hashCode(), 
            groupName, 
            false, 
            false
        );
    }

    private TransactionCategoryResult findResultById(List<TransactionCategoryResult> results, TransactionId id) {
        return results.stream()
            .filter(result -> result.transactionId().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Result not found for transaction ID: " + id));
    }
}