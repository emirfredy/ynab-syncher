package co.personal.ynabsyncher.model.reconciliation;

import co.personal.ynabsyncher.model.AccountId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Request object for transaction reconciliation.
 */
public record ReconciliationRequest(
    AccountId accountId,
    LocalDate fromDate,
    LocalDate toDate,
    ReconciliationStrategy strategy
) {
    public ReconciliationRequest {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(fromDate, "From date cannot be null");
        Objects.requireNonNull(toDate, "To date cannot be null");
        Objects.requireNonNull(strategy, "Reconciliation strategy cannot be null");
        
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
    }

    /**
     * Creates a reconciliation request for a specific account, date range, and strategy.
     */
    public static ReconciliationRequest of(AccountId accountId, LocalDate fromDate, LocalDate toDate, ReconciliationStrategy strategy) {
        return new ReconciliationRequest(accountId, fromDate, toDate, strategy);
    }

    /**
     * Creates a reconciliation request for a specific account and date range using STRICT strategy.
     */
    public static ReconciliationRequest of(AccountId accountId, LocalDate fromDate, LocalDate toDate) {
        return new ReconciliationRequest(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
    }

    /**
     * Creates a reconciliation request for the last 30 days using STRICT strategy.
     */
    public static ReconciliationRequest forLast30Days(AccountId accountId) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);
        return new ReconciliationRequest(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
    }

    /**
     * Creates a reconciliation request for the last 30 days with specified strategy.
     */
    public static ReconciliationRequest forLast30Days(AccountId accountId, ReconciliationStrategy strategy) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);
        return new ReconciliationRequest(accountId, fromDate, toDate, strategy);
    }

    /**
     * Creates a reconciliation request for the current month using STRICT strategy.
     */
    public static ReconciliationRequest forCurrentMonth(AccountId accountId) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = now.withDayOfMonth(1);
        LocalDate toDate = now;
        return new ReconciliationRequest(accountId, fromDate, toDate, ReconciliationStrategy.STRICT);
    }

    /**
     * Creates a reconciliation request for the current month with specified strategy.
     */
    public static ReconciliationRequest forCurrentMonth(AccountId accountId, ReconciliationStrategy strategy) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = now.withDayOfMonth(1);
        LocalDate toDate = now;
        return new ReconciliationRequest(accountId, fromDate, toDate, strategy);
    }

    /**
     * Returns the number of days in this reconciliation period.
     */
    public long getDayCount() {
        return ChronoUnit.DAYS.between(fromDate, toDate) + 1; // +1 to include both start and end dates
    }
}