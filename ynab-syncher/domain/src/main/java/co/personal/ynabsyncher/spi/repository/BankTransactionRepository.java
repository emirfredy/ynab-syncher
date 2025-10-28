package co.personal.ynabsyncher.spi.repository;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for retrieving bank transactions from external data sources.
 * This is a read-only interface - no writing operations are provided as transactions
 * are processed in-memory and not persisted through this repository.
 * This is an outbound port in the hexagonal architecture.
 */
public interface BankTransactionRepository {

    /**
     * Retrieves all bank transactions for a specific account within a date range.
     * 
     * @param accountId the account identifier
     * @param fromDate the start date (inclusive)
     * @param toDate the end date (inclusive)
     * @return list of bank transactions in the specified range
     */
    List<BankTransaction> findByAccountIdAndDateRange(
        AccountId accountId, 
        LocalDate fromDate, 
        LocalDate toDate
    );

    /**
     * Finds bank transactions by their IDs in a batch operation.
     * This is more efficient than multiple individual queries.
     * 
     * @param transactionIds the transaction identifiers
     * @return list of found transactions (may be fewer than requested if some don't exist)
     */
    List<BankTransaction> findByIds(List<TransactionId> transactionIds);
}