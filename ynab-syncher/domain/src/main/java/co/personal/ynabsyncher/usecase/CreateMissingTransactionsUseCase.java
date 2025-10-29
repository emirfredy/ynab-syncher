package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.CreatedTransactionResult;
import co.personal.ynabsyncher.api.dto.UnreconciledTransactionData;
import co.personal.ynabsyncher.api.usecase.CreateMissingTransactions;
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
 * This implementation converts unreconciled bank transaction data into YNAB transactions
 * and creates them using the YNAB API client. It handles both successful creations
 * and failures gracefully, providing detailed results for each transaction.
 */
public class CreateMissingTransactionsUseCase implements CreateMissingTransactions {
    
    private final YnabApiClient ynabApiClient;
    
    public CreateMissingTransactionsUseCase(YnabApiClient ynabApiClient) {
        this.ynabApiClient = Objects.requireNonNull(ynabApiClient, "YNAB API client cannot be null");
    }
    
    @Override
    public CreateMissingTransactionsResponse execute(CreateMissingTransactionsRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        
        List<CreatedTransactionResult> results = new ArrayList<>();
        
        for (UnreconciledTransactionData unreconciledData : request.unreconciledTransactions()) {
            CreatedTransactionResult result = createSingleTransaction(request, unreconciledData);
            results.add(result);
        }
        
        return CreateMissingTransactionsResponse.from(results);
    }
    
    private CreatedTransactionResult createSingleTransaction(
            CreateMissingTransactionsRequest request,
            UnreconciledTransactionData unreconciledData
    ) {
        try {
            BankTransaction bankTransaction = unreconciledData.bankTransaction();
            YnabTransaction ynabTransaction = convertToYnabTransaction(request, unreconciledData);
            
            YnabTransaction createdTransaction = ynabApiClient.createTransaction(
                    request.budgetId().value(),
                    ynabTransaction
            );
            
            return CreatedTransactionResult.success(
                    createdTransaction.id(),
                    bankTransaction.description(),
                    bankTransaction.amount().toDecimal(),
                    bankTransaction.date()
            );
            
        } catch (Exception e) {
            // Generate a temporary transaction ID for failed transactions to maintain consistency
            TransactionId tempId = TransactionId.of("failed-" + System.nanoTime());
            BankTransaction bankTransaction = unreconciledData.bankTransaction();
            
            return CreatedTransactionResult.failure(
                    tempId,
                    bankTransaction.description(),
                    bankTransaction.amount().toDecimal(),
                    bankTransaction.date(),
                    "Failed to create transaction: " + e.getMessage()
            );
        }
    }
    
    private YnabTransaction convertToYnabTransaction(
            CreateMissingTransactionsRequest request,
            UnreconciledTransactionData unreconciledData
    ) {
        BankTransaction bankTransaction = unreconciledData.bankTransaction();
        
        // Use the inferred category from the bank transaction, or create a default "To be Budgeted" category
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