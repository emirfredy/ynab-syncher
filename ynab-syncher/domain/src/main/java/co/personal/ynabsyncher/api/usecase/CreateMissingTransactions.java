package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;

/**
 * Use case for creating missing YNAB transactions from bank transactions missing from YNAB.
 * 
 * This use case takes bank transactions that exist but have no corresponding YNAB transaction
 * and creates them in YNAB to maintain synchronization. Typically used with transactions
 * from ReconciliationResult.missingFromYnab().
 */
public interface CreateMissingTransactions {
    
    /**
     * Creates missing YNAB transactions for bank transactions missing from YNAB.
     * 
     * @param request the request containing budget ID, account IDs, and missing bank transactions
     * @return response with results of transaction creation
     * @throws IllegalArgumentException if the request is invalid
     */
    CreateMissingTransactionsResponse createMissingTransactions(CreateMissingTransactionsRequest request);
}