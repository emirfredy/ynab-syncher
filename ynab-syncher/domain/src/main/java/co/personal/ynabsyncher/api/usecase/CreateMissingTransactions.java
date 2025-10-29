package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;

/**
 * Use case for creating missing YNAB transactions from unreconciled bank transactions.
 * 
 * This use case identifies bank transactions that exist but have no corresponding
 * YNAB transaction and creates them in YNAB to maintain synchronization.
 */
public interface CreateMissingTransactions {
    
    /**
     * Creates missing YNAB transactions for unreconciled bank transactions.
     * 
     * @param request the request containing budget ID, account IDs, and unreconciled transactions
     * @return response with results of transaction creation
     * @throws IllegalArgumentException if the request is invalid
     */
    CreateMissingTransactionsResponse execute(CreateMissingTransactionsRequest request);
}