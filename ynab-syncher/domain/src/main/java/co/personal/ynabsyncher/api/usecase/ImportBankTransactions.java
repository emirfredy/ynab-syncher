package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;

/**
 * Use case interface for importing bank transactions from external data sources.
 * Framework-free port defining the business operation contract.
 */
public interface ImportBankTransactions {
    
    /**
     * Imports bank transactions from the provided data.
     * 
     * @param request the import request containing account ID and transaction data
     * @return the import response with results and statistics
     * @throws IllegalArgumentException if request validation fails
     */
    ImportBankTransactionsResponse importTransactions(ImportBankTransactionsRequest request);
}