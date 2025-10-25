package co.personal.ynabsyncher.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a monetary amount.
 * Follows YNAB's milliunits format where 1000 milliunits = 1 unit of currency.
 */
public record Money(long milliunits) {
    
    /**
     * Creates Money from a decimal amount (e.g., 123.45 becomes 123450 milliunits).
     */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        return new Money(amount.multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue());
    }

    /**
     * Creates Money from a double amount (e.g., 123.45 becomes 123450 milliunits).
     */
    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /**
     * Creates Money directly from milliunits.
     */
    public static Money ofMilliunits(long milliunits) {
        return new Money(milliunits);
    }

    /**
     * Creates zero money.
     */
    public static Money zero() {
        return new Money(0L);
    }

    /**
     * Converts to decimal representation.
     */
    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(milliunits).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }

    /**
     * Returns true if this amount is zero.
     */
    public boolean isZero() {
        return milliunits == 0L;
    }

    /**
     * Returns true if this amount is positive.
     */
    public boolean isPositive() {
        return milliunits > 0L;
    }

    /**
     * Returns true if this amount is negative.
     */
    public boolean isNegative() {
        return milliunits < 0L;
    }

    /**
     * Adds another money amount to this one.
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return new Money(this.milliunits + other.milliunits);
    }

    /**
     * Subtracts another money amount from this one.
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return new Money(this.milliunits - other.milliunits);
    }

    @Override
    public String toString() {
        return toDecimal().toString();
    }
}