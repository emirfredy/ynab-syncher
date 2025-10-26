package co.personal.ynabsyncher.model.bank;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a bank transaction with bank-specific fields.
 * Contains raw transaction data as provided by the bank, with optional inferred category.
 */
public record BankTransaction(
    TransactionId id,
    AccountId accountId,
    LocalDate date,
    Money amount,
    String description,        // Raw bank description
    String merchantName,       // Extracted merchant (optional)
    String memo,              // Bank memo/notes
    String transactionType,   // DEBIT, CREDIT, etc.
    String reference,         // Bank reference number
    Category inferredCategory // Category inferred from description/merchant
) {
    public BankTransaction {
        Objects.requireNonNull(id, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(inferredCategory, "Inferred category cannot be null");
    }

    /**
     * Creates a bank transaction with unknown category (to be inferred later).
     */
    public static BankTransaction withUnknownCategory(
        TransactionId id,
        AccountId accountId,
        LocalDate date,
        Money amount,
        String description,
        String merchantName,
        String memo,
        String transactionType,
        String reference
    ) {
        return new BankTransaction(
            id, accountId, date, amount, description, merchantName, 
            memo, transactionType, reference, Category.unknown()
        );
    }

    /**
     * Creates a new bank transaction with an inferred category.
     */
    public BankTransaction withInferredCategory(Category category) {
        return new BankTransaction(
            id, accountId, date, amount, description, merchantName,
            memo, transactionType, reference, category
        );
    }

    /**
     * Gets the merchant name if available, otherwise returns the description.
     */
    public String displayName() {
        return Optional.ofNullable(merchantName)
                .filter(name -> !name.isBlank())
                .orElse(description);
    }

    /**
     * Checks if this is a debit transaction (money going out).
     */
    public boolean isDebit() {
        return amount.isNegative() || "DEBIT".equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if the category has been inferred (not unknown).
     */
    public boolean hasCategoryInferred() {
        return !inferredCategory.equals(Category.unknown());
    }
}