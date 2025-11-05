package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.TransactionCreationResult;
import co.personal.ynabsyncher.api.usecase.CreateMissingTransactions;
import co.personal.ynabsyncher.ddd.DomainService;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.spi.client.YnabApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of CreateMissingTransactions use case.
 * 
 * This implementation converts bank transactions missing from YNAB into YNAB transactions
 * and creates them using the YNAB API client. It handles both successful creations
 * and failures gracefully, providing detailed results for each transaction.
 * 
 * Typically used with transactions from ReconciliationResult.missingFromYnab().
 */
@DomainService
public class CreateMissingTransactionsUseCase implements CreateMissingTransactions {
    
    private final YnabApiClient ynabApiClient;
    
    public CreateMissingTransactionsUseCase(YnabApiClient ynabApiClient) {
        this.ynabApiClient = Objects.requireNonNull(ynabApiClient, "YNAB API client cannot be null");
    }
    
    @Override
    public CreateMissingTransactionsResponse createMissingTransactions(CreateMissingTransactionsRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        
        List<TransactionCreationResult> results = new ArrayList<>();
        
        for (BankTransaction bankTransaction : request.missingTransactions()) {
            TransactionCreationResult result = createSingleTransaction(request, bankTransaction);
            results.add(result);
        }
        
        return CreateMissingTransactionsResponse.from(results);
    }
    
    private TransactionCreationResult createSingleTransaction(
            CreateMissingTransactionsRequest request,
            BankTransaction bankTransaction
    ) {
        try {
            YnabTransaction ynabTransaction = convertToYnabTransaction(request, bankTransaction);
            
            YnabTransaction createdTransaction = ynabApiClient.createTransaction(
                    request.budgetId().value(),
                    ynabTransaction
            );
            
            return TransactionCreationResult.success(
                    bankTransaction,
                    createdTransaction.id()
            );
            
        } catch (Exception e) {
            return TransactionCreationResult.failure(
                    bankTransaction,
                    "Failed to create transaction: " + e.getMessage()
            );
        }
    }
    
    private YnabTransaction convertToYnabTransaction(
            CreateMissingTransactionsRequest request,
            BankTransaction bankTransaction
    ) {
        // Use the inferred category from the bank transaction
        Category category = bankTransaction.inferredCategory();
        
        // Generate a new transaction ID for the YNAB transaction
        TransactionId newTransactionId = TransactionId.of("temp-" + System.nanoTime());
        
        return new YnabTransaction(
                newTransactionId,
                request.ynabAccountId(),
                bankTransaction.date(),
                bankTransaction.amount(),
                bankTransaction.merchantName(), // Use merchant name as payee if available
                bankTransaction.memo(),
                category,
                ClearedStatus.UNCLEARED, // New transactions start as uncleared
                true, // Auto-approve imported transactions
                null // No flag color initially
        );
    }
}