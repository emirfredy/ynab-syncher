package co.personal.ynabsyncher.spi.repository;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Transaction;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for retrieving YNAB transactions.
 * This is an outbound port in the hexagonal architecture.
 */
public interface YnabTransactionRepository {

    /**
     * Retrieves all YNAB transactions for a specific account within a date range.
     * 
     * @param accountId the account identifier
     * @param fromDate the start date (inclusive)
     * @param toDate the end date (inclusive)
     * @return list of YNAB transactions
     */
    List<Transaction> findByAccountIdAndDateRange(AccountId accountId, LocalDate fromDate, LocalDate toDate);

    /**
     * Retrieves all YNAB transactions for a specific account.
     * 
     * @param accountId the account identifier
     * @return list of all YNAB transactions for the account
     */
    List<Transaction> findByAccountId(AccountId accountId);
}