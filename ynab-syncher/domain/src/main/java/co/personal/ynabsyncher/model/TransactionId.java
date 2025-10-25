package co.personal.ynabsyncher.model;

import java.util.Objects;

/**
 * Value object representing a unique transaction identifier.
 */
public record TransactionId(String value) {
    public TransactionId {
        Objects.requireNonNull(value, "Transaction ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID value cannot be empty");
        }
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}