package co.personal.ynabsyncher.model.ynab;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a YNAB transaction with YNAB-specific categorization.
 * Contains budget-specific fields like category and cleared status.
 */
public record YnabTransaction(
    TransactionId id,
    AccountId accountId,
    LocalDate date,
    Money amount,
    String payeeName,
    String memo,
    Category category,             // YNAB category assignment
    ClearedStatus clearedStatus,   // CLEARED, UNCLEARED, RECONCILED
    boolean approved,             // Whether transaction is approved in YNAB
    String flagColor              // YNAB flag color (optional)
) {
    public YnabTransaction {
        Objects.requireNonNull(id, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(clearedStatus, "Cleared status cannot be null");
    }

    /**
     * Checks if this transaction is reconciled in YNAB.
     */
    public boolean isReconciled() {
        return clearedStatus == ClearedStatus.RECONCILED;
    }

    /**
     * Gets the display name for reconciliation purposes.
     */
    public String displayName() {
        return Optional.ofNullable(payeeName)
                .filter(name -> !name.isBlank())
                .orElse("Unknown Payee");
    }
}