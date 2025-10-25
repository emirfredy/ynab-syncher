package co.personal.ynabsyncher.model;

import java.util.Objects;

/**
 * Value object representing a unique account identifier.
 */
public record AccountId(String value) {
    public AccountId {
        Objects.requireNonNull(value, "Account ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID value cannot be empty");
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}