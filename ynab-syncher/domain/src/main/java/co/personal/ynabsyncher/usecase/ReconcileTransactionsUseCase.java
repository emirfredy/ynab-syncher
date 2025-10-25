package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.model.*;
import co.personal.ynabsyncher.model.matcher.TransactionMatcher;
import co.personal.ynabsyncher.model.matcher.TransactionMatcherFactory;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationSummary;
import co.personal.ynabsyncher.spi.repository.BankTransactionRepository;
import co.personal.ynabsyncher.spi.repository.YnabTransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the ReconcileTransactions use case.
 * This class contains the business logic for reconciling transactions between YNAB and bank accounts.
 */
public class ReconcileTransactionsUseCase implements ReconcileTransactions {

    private final YnabTransactionRepository ynabTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public ReconcileTransactionsUseCase(
        YnabTransactionRepository ynabTransactionRepository,
        BankTransactionRepository bankTransactionRepository
    ) {
        this.ynabTransactionRepository = Objects.requireNonNull(ynabTransactionRepository, 
            "YNAB transaction repository cannot be null");
        this.bankTransactionRepository = Objects.requireNonNull(bankTransactionRepository, 
            "Bank transaction repository cannot be null");
    }

    @Override
    public ReconciliationResult reconcile(ReconciliationRequest request) {
        Objects.requireNonNull(request, "Reconciliation request cannot be null");

        // Create the appropriate matcher for the requested strategy
        TransactionMatcher matcher = TransactionMatcherFactory.createMatcher(request.strategy());

        // Fetch transactions from both sources
        List<Transaction> ynabTransactions = ynabTransactionRepository
            .findByAccountIdAndDateRange(request.accountId(), request.fromDate(), request.toDate());
        
        List<Transaction> bankTransactions = bankTransactionRepository
            .findByAccountIdAndDateRange(request.accountId(), request.fromDate(), request.toDate());

        // Reconcile transactions using the specified strategy
        return performReconciliation(request, ynabTransactions, bankTransactions, matcher);
    }

    private ReconciliationResult performReconciliation(
        ReconciliationRequest request,
        List<Transaction> ynabTransactions,
        List<Transaction> bankTransactions,
        TransactionMatcher matcher
    ) {
        List<Transaction> matchedTransactions = new ArrayList<>();
        List<Transaction> missingFromYnab = new ArrayList<>();

        // For each bank transaction, try to find a matching YNAB transaction
        for (Transaction bankTransaction : bankTransactions) {
            boolean foundMatch = false;
            
            for (Transaction ynabTransaction : ynabTransactions) {
                if (matcher.matches(bankTransaction, ynabTransaction)) {
                    matchedTransactions.add(bankTransaction);
                    foundMatch = true;
                    break; // Stop looking once we find a match
                }
            }
            
            if (!foundMatch) {
                missingFromYnab.add(bankTransaction);
            }
        }

        // Create summary
        ReconciliationSummary summary = new ReconciliationSummary(
            request.accountId(),
            LocalDate.now(),
            request.fromDate(),
            request.toDate(),
            request.strategy(),
            bankTransactions.size(),
            ynabTransactions.size(),
            matchedTransactions.size(),
            missingFromYnab.size()
        );

        return new ReconciliationResult(missingFromYnab, matchedTransactions, summary);
    }
}