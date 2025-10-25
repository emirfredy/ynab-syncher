package co.personal.ynabsyncher.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a financial transaction in the domain model.
 * This is a common abstraction that can represent both YNAB and bank transactions.
 */
public record Transaction(
    TransactionId id,
    AccountId accountId,
    LocalDate date,
    Money amount,
    String payeeName,
    String memo,
    TransactionSource source
) {
    public Transaction {
        Objects.requireNonNull(id, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(source, "Source cannot be null");
    }

    /**
     * Creates a YNAB transaction with the given details.
     */
    public static Transaction ynabTransaction(
        TransactionId id,
        AccountId accountId,
        LocalDate date,
        Money amount,
        String payeeName,
        String memo
    ) {
        return new Transaction(id, accountId, date, amount, payeeName, memo, TransactionSource.YNAB);
    }

    /**
     * Creates a bank transaction with the given details.
     */
    public static Transaction bankTransaction(
        TransactionId id,
        AccountId accountId,
        LocalDate date,
        Money amount,
        String payeeName,
        String memo
    ) {
        return new Transaction(id, accountId, date, amount, payeeName, memo, TransactionSource.BANK);
    }
}