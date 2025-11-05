package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.ddd.DomainService;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.matcher.TransactionMatcher;
import co.personal.ynabsyncher.model.matcher.TransactionMatcherFactory;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationSummary;
import co.personal.ynabsyncher.model.reconciliation.TransactionMatchResult;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.service.TransactionReconciliationService;
import co.personal.ynabsyncher.service.CategoryInferenceService;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the ReconcileTransactions use case.
 * This class contains the business logic for reconciling transactions between YNAB and bank accounts,
 * including category inference for uncategorized bank transactions.
 */
@DomainService
public class ReconcileTransactionsUseCase implements ReconcileTransactions {

    private final YnabTransactionRepository ynabTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final YnabCategoryRepository ynabCategoryRepository;
    private final TransactionReconciliationService reconciliationService;
    private final CategoryInferenceService categoryInferenceService;

    public ReconcileTransactionsUseCase(
        YnabTransactionRepository ynabTransactionRepository,
        BankTransactionRepository bankTransactionRepository,
        YnabCategoryRepository ynabCategoryRepository,
        TransactionReconciliationService reconciliationService,
        CategoryInferenceService categoryInferenceService
    ) {
        this.ynabTransactionRepository = Objects.requireNonNull(ynabTransactionRepository, 
            "YNAB transaction repository cannot be null");
        this.bankTransactionRepository = Objects.requireNonNull(bankTransactionRepository, 
            "Bank transaction repository cannot be null");
        this.ynabCategoryRepository = Objects.requireNonNull(ynabCategoryRepository,
            "YNAB category repository cannot be null");
        this.reconciliationService = Objects.requireNonNull(reconciliationService,
            "Transaction reconciliation service cannot be null");
        this.categoryInferenceService = Objects.requireNonNull(categoryInferenceService,
            "Category inference service cannot be null");
    }

    @Override
    public ReconciliationResult reconcile(ReconciliationRequest request) {
        Objects.requireNonNull(request, "Reconciliation request cannot be null");

        // Create the appropriate matcher for the requested strategy
        TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(request.strategy());

        // Fetch transactions from both sources
        List<YnabTransaction> ynabTransactions = ynabTransactionRepository
            .findByAccountIdAndDateRange(request.accountId(), request.fromDate(), request.toDate());
        
        List<BankTransaction> bankTransactions = bankTransactionRepository
            .findByAccountIdAndDateRange(request.accountId(), request.fromDate(), request.toDate());

        // Perform category inference on bank transactions that need it
        List<BankTransaction> enrichedBankTransactions = enrichTransactionsWithCategoryInference(bankTransactions);

        // Reconcile transactions using the specified strategy
        return performReconciliation(request, ynabTransactions, enrichedBankTransactions, matcher);
    }

    /**
     * Enriches bank transactions with category inference for those without categories.
     */
    private List<BankTransaction> enrichTransactionsWithCategoryInference(List<BankTransaction> bankTransactions) {
        return bankTransactions.stream()
                .map(this::inferCategoryIfNeeded)
                .toList();
    }

    /**
     * Infers category for a bank transaction if it doesn't already have one.
     * Returns the transaction with inferred category (if found) without persisting changes.
     */
    private BankTransaction inferCategoryIfNeeded(BankTransaction transaction) {
        // Skip if already has a category
        if (transaction.hasCategoryInferred()) {
            return transaction;
        }

        try {
            List<YnabCategory> availableCategories = ynabCategoryRepository.findAllAvailableCategories();
            return categoryInferenceService.analyzeTransaction(transaction, availableCategories)
                .filter(inferenceResult -> inferenceResult.hasMatch())
                .map(inferenceResult -> transaction.withInferredCategory(inferenceResult.category()))
                .orElse(transaction);
        } catch (Exception e) {
            // Log error but don't fail the reconciliation
            // In a real implementation, we'd use proper logging here
            System.err.println("Failed to infer category for transaction " + transaction.id() + ": " + e.getMessage());
            return transaction;
        }
    }

    private ReconciliationResult performReconciliation(
        ReconciliationRequest request,
        List<YnabTransaction> ynabTransactions,
        List<BankTransaction> bankTransactions,
        TransactionMatcher matcher
    ) {
        // Delegate the complex matching logic to the domain service
        TransactionMatchResult matchResult = reconciliationService.reconcileTransactions(
            bankTransactions,
            ynabTransactions,
            matcher
        );

        // Create summary from the match results
        ReconciliationSummary summary = new ReconciliationSummary(
            request.accountId(),
            LocalDate.now(),
            request.fromDate(),
            request.toDate(),
            request.strategy(),
            bankTransactions.size(),
            ynabTransactions.size(),
            matchResult.getMatchedCount(),
            matchResult.getMissingFromYnabCount()
        );

        return new ReconciliationResult(matchResult.missingFromYnab(), matchResult.matchedTransactions(), summary);
    }
}