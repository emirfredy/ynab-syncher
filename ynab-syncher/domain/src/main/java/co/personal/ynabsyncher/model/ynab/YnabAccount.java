package co.personal.ynabsyncher.model.ynab;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Money;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represents a YNAB account with balance and status information.
 * Contains account-level metadata needed for transaction synchronization.
 */
public record YnabAccount(
    AccountId id,
    String name,
    AccountType type,
    boolean onBudget,
    boolean closed,
    String note,
    Money balance,
    Money clearedBalance,
    Money unclearedBalance,
    String transferPayeeId,
    boolean directImportLinked,
    String directImportInError,
    OffsetDateTime lastReconciledAt
) {
    public YnabAccount {
        Objects.requireNonNull(id, "Account ID cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
        Objects.requireNonNull(type, "Account type cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(clearedBalance, "Cleared balance cannot be null");
        Objects.requireNonNull(unclearedBalance, "Uncleared balance cannot be null");
        
        if (name.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be blank");
        }
    }

    /**
     * Checks if this account is available for transaction imports.
     * Must be on-budget and not closed.
     */
    public boolean isImportable() {
        return onBudget && !closed;
    }

    /**
     * Checks if this account has direct import configured and working.
     */
    public boolean hasWorkingDirectImport() {
        return directImportLinked && 
               (directImportInError == null || directImportInError.isBlank());
    }

    /**
     * Gets the display name for the account.
     */
    public String displayName() {
        return closed ? name + " (Closed)" : name;
    }

    /**
     * YNAB account types as defined by the API.
     */
    public enum AccountType {
        CHECKING,
        SAVINGS,
        CASH,
        CREDIT_CARD,
        LINE_OF_CREDIT,
        OTHER_ASSET,
        OTHER_LIABILITY,
        MORTGAGE,
        AUTO_LOAN,
        STUDENT_LOAN,
        PERSONAL_LOAN,
        MEDICAL_DEBT,
        OTHER_DEBT
    }
}