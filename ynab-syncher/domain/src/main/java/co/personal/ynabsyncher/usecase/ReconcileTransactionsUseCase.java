package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.matcher.TransactionMatcher;
import co.personal.ynabsyncher.model.matcher.TransactionMatcherFactory;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationSummary;
import co.personal.ynabsyncher.model.reconciliation.TransactionMatchResult;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.service.TransactionReconciliationService;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the ReconcileTransactions use case.
 * This class contains the business logic for reconciling transactions between YNAB and bank accounts.
 */
public class ReconcileTransactionsUseCase implements ReconcileTransactions {

    private final YnabTransactionRepository ynabTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final TransactionReconciliationService reconciliationService;

    public ReconcileTransactionsUseCase(
        YnabTransactionRepository ynabTransactionRepository,
        BankTransactionRepository bankTransactionRepository,
        TransactionReconciliationService reconciliationService
    ) {
        this.ynabTransactionRepository = Objects.requireNonNull(ynabTransactionRepository, 
            "YNAB transaction repository cannot be null");
        this.bankTransactionRepository = Objects.requireNonNull(bankTransactionRepository, 
            "Bank transaction repository cannot be null");
        this.reconciliationService = Objects.requireNonNull(reconciliationService,
            "Transaction reconciliation service cannot be null");
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

        // Reconcile transactions using the specified strategy
        return performReconciliation(request, ynabTransactions, bankTransactions, matcher);
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