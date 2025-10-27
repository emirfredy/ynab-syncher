package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse.TransactionCategoryResult;
import co.personal.ynabsyncher.api.usecase.InferTransactionCategories;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private CategoryMappingRepository categoryMappingRepository;
    
    @Mock
    private CategoryInferenceService categoryInferenceService;
    
    private InferTransactionCategories inferTransactionCategories;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Default mock: return empty mappings so service falls back to similarity matching
        when(categoryMappingRepository.findMappingsForPattern(any()))
            .thenReturn(List.of());
            
        inferTransactionCategories = new InferTransactionCategoriesUseCase(
            bankTransactionRepository, 
            ynabCategoryRepository,
            categoryMappingRepository,
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
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories), any()))
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
            
            verify(categoryInferenceService).analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories), any());
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
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories), any()))
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
            
            verify(categoryInferenceService).analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories), any());
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
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction1), eq(availableCategories), any()))
                .thenReturn(Optional.of(successfulResult));
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction2), eq(availableCategories), any()))
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
            verify(categoryInferenceService).analyzeTransaction(eq(uncategorizedTransaction1), eq(availableCategories), any());
            verify(categoryInferenceService).analyzeTransaction(eq(uncategorizedTransaction2), eq(availableCategories), any());
            verify(categoryInferenceService, times(2)).analyzeTransaction(any(), any(), any());
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
            when(categoryInferenceService.analyzeTransaction(eq(transaction1), eq(availableCategories), any()))
                .thenReturn(Optional.of(result1));
            when(categoryInferenceService.analyzeTransaction(eq(transaction2), eq(availableCategories), any()))
                .thenReturn(Optional.of(result2));
            when(categoryInferenceService.analyzeTransaction(eq(transaction3), eq(availableCategories), any()))
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
            verify(categoryInferenceService).analyzeTransaction(eq(transaction1), eq(availableCategories), any());
            verify(categoryInferenceService).analyzeTransaction(eq(transaction2), eq(availableCategories), any());
            verify(categoryInferenceService).analyzeTransaction(eq(transaction3), eq(availableCategories), any());
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
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(List.of()), any()))
                .thenReturn(Optional.empty());
            
            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);
            
            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(0);
            assertThat(response.failedInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).successful()).isFalse();
            
            verify(categoryInferenceService).analyzeTransaction(eq(uncategorizedTransaction), eq(List.of()), any());
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
            when(categoryInferenceService.analyzeTransaction(eq(uncategorizedTransaction), eq(availableCategories), any()))
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
            when(categoryInferenceService.analyzeTransaction(eq(highConfidenceTransaction), eq(availableCategories), any()))
                .thenReturn(Optional.of(highConfidenceResult));
            when(categoryInferenceService.analyzeTransaction(eq(mediumConfidenceTransaction), eq(availableCategories), any()))
                .thenReturn(Optional.of(mediumConfidenceResult));
            when(categoryInferenceService.analyzeTransaction(eq(lowConfidenceTransaction), eq(availableCategories), any()))
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

    @Nested
    @DisplayName("Learned Mapping Integration")
    class LearnedMappingIntegration {

        @Test
        @DisplayName("Should prioritize exact learned mappings over similarity matching")
        void shouldPrioritizeExactLearnedMappingsOverSimilarityMatching() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-1");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction transaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("STARBUCKS COFFEE")
                .withDescription("Morning coffee")
                .withInferredCategory(Category.unknown())
                .build();

            // Available categories
            YnabCategory diningCategory = createYnabCategory("Dining Out", "Food & Dining");
            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(diningCategory, groceriesCategory);

            // Create learned mapping that should override similarity matching
            TransactionPattern pattern = TransactionPattern.fromBankTransaction(transaction);
            CategoryMapping learnedMapping = CategoryMapping.fromSuccessfulMatch(
                pattern, 
                Category.ynabCategory("dining_out", "Dining Out")
            );
            List<CategoryMapping> learnedMappings = List.of(learnedMapping);

            // High confidence result from exact matching (should get +0.2 boost)
            CategoryInferenceResult exactMatchResult = new CategoryInferenceResult(
                Category.ynabCategory("dining_out", "Dining Out"), 
                1.0, // High confidence from exact match
                "Exact pattern match from learned mapping"
            );

            // Mock repository calls
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(transaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(pattern))
                .thenReturn(learnedMappings);
            when(categoryInferenceService.analyzeTransaction(eq(transaction), eq(availableCategories), eq(learnedMappings)))
                .thenReturn(Optional.of(exactMatchResult));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);

            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.inferenceResult().category().name()).isEqualTo("Dining Out");
            assertThat(result.inferenceResult().confidence()).isEqualTo(1.0);
            assertThat(result.inferenceResult().reasoning()).contains("Exact pattern match");

            // Verify learned mappings were queried
            verify(categoryMappingRepository).findMappingsForPattern(pattern);
        }

        @Test
        @DisplayName("Should fall back to similarity when no learned mappings exist")
        void shouldFallBackToSimilarityWhenNoLearnedMappingsExist() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-2");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction transaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("NEW GROCERY STORE")
                .withDescription("Weekly shopping")
                .withInferredCategory(Category.unknown())
                .build();

            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(groceriesCategory);

            // No learned mappings exist
            List<CategoryMapping> emptyMappings = List.of();

            // Fallback similarity result (confidence reduced by 0.8 multiplier)
            CategoryInferenceResult similarityResult = new CategoryInferenceResult(
                Category.ynabCategory("groceries", "Groceries"), 
                0.6, // Lower confidence from similarity matching
                "Fallback similarity match: Merchant name match"
            );

            // Mock repository calls
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(transaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(emptyMappings);
            when(categoryInferenceService.analyzeTransaction(eq(transaction), eq(availableCategories), eq(emptyMappings)))
                .thenReturn(Optional.of(similarityResult));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);

            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.inferenceResult().category().name()).isEqualTo("Groceries");
            assertThat(result.inferenceResult().confidence()).isEqualTo(0.6);
            assertThat(result.inferenceResult().reasoning()).contains("Fallback similarity match");
        }

        @Test
        @DisplayName("Should handle multiple competing learned mappings")
        void shouldHandleMultipleCompetingLearnedMappings() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-3");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction transaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("COFFEE SHOP")
                .withDescription("Quick purchase")
                .withInferredCategory(Category.unknown())
                .build();

            YnabCategory diningCategory = createYnabCategory("Dining Out", "Food & Dining");
            YnabCategory groceriesCategory = createYnabCategory("Groceries", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(diningCategory, groceriesCategory);

            // Multiple learned mappings with different confidence levels
            TransactionPattern pattern = TransactionPattern.fromBankTransaction(transaction);
            CategoryMapping highConfidenceMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory("dining_out", "Dining Out"),
                pattern.textPatterns(),
                0.9,
                5 // Higher occurrence count
            );
            CategoryMapping lowConfidenceMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                Category.ynabCategory("groceries", "Groceries"),
                pattern.textPatterns(),
                0.6,
                2 // Lower occurrence count
            );
            List<CategoryMapping> competingMappings = List.of(highConfidenceMapping, lowConfidenceMapping);

            // Service should select the highest confidence mapping
            CategoryInferenceResult bestResult = new CategoryInferenceResult(
                Category.ynabCategory("dining_out", "Dining Out"), 
                0.9,
                "Exact pattern match: highest confidence mapping selected"
            );

            // Mock repository calls
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(transaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(pattern))
                .thenReturn(competingMappings);
            when(categoryInferenceService.analyzeTransaction(eq(transaction), eq(availableCategories), eq(competingMappings)))
                .thenReturn(Optional.of(bestResult));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);

            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.inferenceResult().category().name()).isEqualTo("Dining Out");
            assertThat(result.inferenceResult().confidence()).isEqualTo(0.9);
        }
    }

    @Nested
    @DisplayName("Repository Integration Edge Cases")
    class RepositoryIntegrationEdgeCases {

        @Test
        @DisplayName("Should handle repository failures gracefully")
        void shouldHandleRepositoryFailuresGracefully() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-failure");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);

            // Mock repository failure
            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then - Should handle gracefully
            assertThatThrownBy(() -> inferTransactionCategories.inferCategories(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

            // Verify no unnecessary calls were made after failure
            verifyNoInteractions(ynabCategoryRepository);
            verifyNoInteractions(categoryInferenceService);
        }

        @Test
        @DisplayName("Should handle non-existent transaction IDs")
        void shouldHandleNonExistentTransactionIds() {
            // Given
            TransactionId existingId = TransactionId.of("tx-exists");
            TransactionId nonExistentId = TransactionId.of("tx-missing");
            CategoryInferenceRequest request = new CategoryInferenceRequest(
                List.of(existingId, nonExistentId)
            );

            BankTransaction existingTransaction = aBankTransaction()
                .withId(existingId)
                .withMerchantName("COFFEE SHOP")
                .withInferredCategory(Category.unknown())
                .build();

            // Repository returns only existing transaction
            when(bankTransactionRepository.findByIds(List.of(existingId, nonExistentId)))
                .thenReturn(List.of(existingTransaction)); // Missing nonExistentId

            YnabCategory category = createYnabCategory("Dining Out", "Food & Dining");
            List<YnabCategory> availableCategories = List.of(category);

            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(new CategoryInferenceResult(
                    Category.ynabCategory("dining_out", "Dining Out"), 0.8, "Match found")));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then - Should only process existing transaction
            assertThat(response.processedCount()).isEqualTo(1); // Only existing transaction processed
            assertThat(response.successfulInferences()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).transactionId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("Should handle empty transaction list")
        void shouldHandleEmptyTransactionList() {
            // Given - Empty transaction list should throw IllegalArgumentException at request creation
            List<TransactionId> emptyList = List.of();

            // When/Then - Should throw exception during request creation
            assertThatThrownBy(() -> new CategoryInferenceRequest(emptyList))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction IDs cannot be empty");

            // Verify no repository calls were made
            verifyNoInteractions(bankTransactionRepository);
            verifyNoInteractions(ynabCategoryRepository);
            verifyNoInteractions(categoryInferenceService);
        }
    }

    @Nested
    @DisplayName("Performance and Boundary Conditions")
    class PerformanceAndBoundaryConditions {

        @Test
        @DisplayName("Should handle large batch efficiently")
        void shouldHandleLargeBatchEfficiently() {
            // Given - Create a large batch of transactions
            List<TransactionId> transactionIds = List.of(
                TransactionId.of("tx-1"), TransactionId.of("tx-2"), TransactionId.of("tx-3"),
                TransactionId.of("tx-4"), TransactionId.of("tx-5")
            );
            CategoryInferenceRequest request = new CategoryInferenceRequest(transactionIds);

            List<BankTransaction> transactions = transactionIds.stream()
                .map(id -> aBankTransaction()
                    .withId(id)
                    .withMerchantName("STORE " + id.value())
                    .withInferredCategory(Category.unknown())
                    .build())
                .toList();

            YnabCategory category = createYnabCategory("Shopping", "General");
            List<YnabCategory> availableCategories = List.of(category);

            CategoryInferenceResult successResult = new CategoryInferenceResult(
                Category.ynabCategory("shopping", "Shopping"), 0.7, "Batch processed");

            // Mock repository calls - verify single batch call, not N+1
            when(bankTransactionRepository.findByIds(transactionIds))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(successResult));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then
            assertThat(response.processedCount()).isEqualTo(5);
            assertThat(response.successfulInferences()).isEqualTo(5);
            assertThat(response.results()).hasSize(5);

            // Verify single batch calls (no N+1 queries)
            verify(bankTransactionRepository, times(1)).findByIds(transactionIds);
            verify(ynabCategoryRepository, times(1)).findAllAvailableCategories();
            verify(categoryInferenceService, times(5)).analyzeTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("Should maintain transaction order in results")
        void shouldMaintainTransactionOrderInResults() {
            // Given
            List<TransactionId> orderedIds = List.of(
                TransactionId.of("tx-first"),
                TransactionId.of("tx-second"),
                TransactionId.of("tx-third")
            );
            CategoryInferenceRequest request = new CategoryInferenceRequest(orderedIds);

            List<BankTransaction> transactions = orderedIds.stream()
                .map(id -> aBankTransaction()
                    .withId(id)
                    .withMerchantName("MERCHANT " + id.value())
                    .withInferredCategory(Category.unknown())
                    .build())
                .toList();

            YnabCategory category = createYnabCategory("General", "Uncategorized");
            List<YnabCategory> availableCategories = List.of(category);

            when(bankTransactionRepository.findByIds(orderedIds))
                .thenReturn(transactions);
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(new CategoryInferenceResult(
                    Category.ynabCategory("general", "General"), 0.5, "Order test")));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then - Results should maintain input order
            assertThat(response.results()).hasSize(3);
            assertThat(response.results().get(0).transactionId()).isEqualTo(TransactionId.of("tx-first"));
            assertThat(response.results().get(1).transactionId()).isEqualTo(TransactionId.of("tx-second"));
            assertThat(response.results().get(2).transactionId()).isEqualTo(TransactionId.of("tx-third"));
        }
    }

    @Nested
    @DisplayName("Domain Rule Enforcement")
    class DomainRuleEnforcement {

        @Test
        @DisplayName("Should enforce minimum confidence thresholds")
        void shouldEnforceMinimumConfidenceThresholds() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-low-confidence");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction transaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("UNKNOWN MERCHANT")
                .withInferredCategory(Category.unknown())
                .build();

            YnabCategory category = createYnabCategory("Miscellaneous", "General");
            List<YnabCategory> availableCategories = List.of(category);

            // Service returns very low confidence result (below threshold)
            CategoryInferenceResult lowConfidenceResult = new CategoryInferenceResult(
                Category.ynabCategory("misc", "Miscellaneous"), 
                0.1, // Very low confidence
                "Weak similarity match"
            );

            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(transaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(lowConfidenceResult));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then - Should still process but with low confidence indicator
            assertThat(response.processedCount()).isEqualTo(1);
            assertThat(response.successfulInferences()).isEqualTo(1);

            TransactionCategoryResult result = response.results().get(0);
            assertThat(result.successful()).isTrue();
            assertThat(result.inferenceResult().confidence()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should validate transaction state consistency")
        void shouldValidateTransactionStateConsistency() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-state-check");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            // Transaction with unknown category but hasCategoryInferred() returns true (inconsistent state)
            BankTransaction inconsistentTransaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("TEST MERCHANT")
                .withInferredCategory(Category.unknown()) // Unknown category
                .build();

            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(inconsistentTransaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(List.of(createYnabCategory("Test", "Test Group")));
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(new CategoryInferenceResult(
                    Category.ynabCategory("test", "Test"), 0.5, "State check")));

            // When
            CategoryInferenceResponse response = inferTransactionCategories.inferCategories(request);

            // Then - Should recognize transaction needs inference despite potential state inconsistency
            assertThat(response.processedCount()).isEqualTo(1);
            verify(ynabCategoryRepository).findAllAvailableCategories();
            verify(categoryInferenceService).analyzeTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("Should produce deterministic results for same input")
        void shouldProduceDeterministicResultsForSameInput() {
            // Given
            TransactionId transactionId = TransactionId.of("tx-deterministic");
            CategoryInferenceRequest request = CategoryInferenceRequest.forSingleTransaction(transactionId);
            
            BankTransaction transaction = aBankTransaction()
                .withId(transactionId)
                .withMerchantName("DETERMINISTIC TEST")
                .withInferredCategory(Category.unknown())
                .build();

            YnabCategory category = createYnabCategory("Test Category", "Test Group");
            List<YnabCategory> availableCategories = List.of(category);

            CategoryInferenceResult deterministicResult = new CategoryInferenceResult(
                Category.ynabCategory("test", "Test Category"), 0.75, "Deterministic match");

            when(bankTransactionRepository.findByIds(List.of(transactionId)))
                .thenReturn(List.of(transaction));
            when(ynabCategoryRepository.findAllAvailableCategories())
                .thenReturn(availableCategories);
            when(categoryMappingRepository.findMappingsForPattern(any()))
                .thenReturn(List.of());
            when(categoryInferenceService.analyzeTransaction(any(), any(), any()))
                .thenReturn(Optional.of(deterministicResult));

            // When - Call multiple times
            CategoryInferenceResponse response1 = inferTransactionCategories.inferCategories(request);
            CategoryInferenceResponse response2 = inferTransactionCategories.inferCategories(request);

            // Then - Results should be identical (idempotency)
            assertThat(response1.processedCount()).isEqualTo(response2.processedCount());
            assertThat(response1.successfulInferences()).isEqualTo(response2.successfulInferences());
            assertThat(response1.results().get(0).inferenceResult().category())
                .isEqualTo(response2.results().get(0).inferenceResult().category());
            assertThat(response1.results().get(0).inferenceResult().confidence())
                .isEqualTo(response2.results().get(0).inferenceResult().confidence());
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