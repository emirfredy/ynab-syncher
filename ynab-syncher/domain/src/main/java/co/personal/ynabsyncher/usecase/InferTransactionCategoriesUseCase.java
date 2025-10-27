package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse.TransactionCategoryResult;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResultDto;
import co.personal.ynabsyncher.api.usecase.InferTransactionCategories;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import co.personal.ynabsyncher.service.CategoryInferenceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of category inference use case.
 * 
 * Responsibility: Infer categories for bank transactions without persistence.
 * This is a read-only operation that provides inference results for downstream processes.
 * 
 * Architecture note: Since BankTransactionRepository is read-only (external data source),
 * this use case only performs inference and returns results without saving anything.
 */
public class InferTransactionCategoriesUseCase implements InferTransactionCategories {
    
    private final BankTransactionRepository bankTransactionRepository;
    private final YnabCategoryRepository ynabCategoryRepository;
    private final CategoryInferenceService categoryInferenceService;
    
    public InferTransactionCategoriesUseCase(
            BankTransactionRepository bankTransactionRepository,
            YnabCategoryRepository ynabCategoryRepository,
            CategoryInferenceService categoryInferenceService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.ynabCategoryRepository = ynabCategoryRepository;
        this.categoryInferenceService = categoryInferenceService;
    }
    
    @Override
    public CategoryInferenceResponse inferCategories(CategoryInferenceRequest request) {
        // Batch fetch transactions and categories
        List<BankTransaction> transactions = bankTransactionRepository.findByIds(request.transactionIds());
        List<YnabCategory> availableCategories = ynabCategoryRepository.findAllAvailableCategories();
        
        // Filter out transactions that already have inferred categories
        List<BankTransaction> uncategorizedTransactions = transactions.stream()
                .filter(transaction -> !transaction.hasCategoryInferred())
                .toList();
        
        if (uncategorizedTransactions.isEmpty()) {
            return createResponseAllAlreadyCategorized(transactions);
        }
        
        // Process each uncategorized transaction
        List<TransactionCategoryResult> results = new ArrayList<>();
        
        for (BankTransaction transaction : transactions) {
            if (transaction.hasCategoryInferred()) {
                // Already categorized - create result from existing data
                var dtoResult = createInferenceResultFromTransaction(transaction);
                results.add(TransactionCategoryResult.success(transaction.id(), dtoResult));
            } else {
                // Needs inference - pass categories to service
                Optional<co.personal.ynabsyncher.model.CategoryInferenceResult> inferenceResult = 
                        categoryInferenceService.analyzeTransaction(transaction, availableCategories);
                
                if (inferenceResult.isPresent() && inferenceResult.get().hasMatch()) {
                    var dtoResult = CategoryInferenceResultDto.fromDomain(inferenceResult.get());
                    results.add(TransactionCategoryResult.success(transaction.id(), dtoResult));
                } else {
                    results.add(TransactionCategoryResult.failure(transaction.id()));
                }
            }
        }
        
        int successful = (int) results.stream().filter(TransactionCategoryResult::successful).count();
        int failed = results.size() - successful;
        
        return new CategoryInferenceResponse(results, results.size(), successful, failed);
    }
    
    private CategoryInferenceResponse createResponseAllAlreadyCategorized(List<BankTransaction> transactions) {
        List<TransactionCategoryResult> results = new ArrayList<>();
        
        for (BankTransaction transaction : transactions) {
            var dtoResult = createInferenceResultFromTransaction(transaction);
            results.add(TransactionCategoryResult.success(transaction.id(), dtoResult));
        }
        
        return new CategoryInferenceResponse(results, results.size(), results.size(), 0);
    }
    
    private CategoryInferenceResultDto createInferenceResultFromTransaction(BankTransaction transaction) {
        if (!transaction.hasCategoryInferred()) {
            throw new IllegalArgumentException("Transaction must have inferred category");
        }
        
        var category = transaction.inferredCategory();
        return new CategoryInferenceResultDto(
                category,
                1.0, // Assume full confidence for already-stored categories
                "Previously inferred"
        );
    }
}
