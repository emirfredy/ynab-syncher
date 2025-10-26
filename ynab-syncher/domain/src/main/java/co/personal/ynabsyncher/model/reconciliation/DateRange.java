package co.personal.ynabsyncher.model.reconciliation;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a date range for transaction matching optimization.
 * Used to constrain matching operations to transactions within a specific time window.
 */
public record DateRange(LocalDate start, LocalDate end) {
    
    public DateRange {
        Objects.requireNonNull(start, "Start date cannot be null");
        Objects.requireNonNull(end, "End date cannot be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    /**
     * Checks if the given date falls within this range (inclusive).
     */
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Returns the number of days in this range (inclusive).
     */
    public long getDayCount() {
        return start.until(end).getDays() + 1;
    }

    /**
     * Creates a date range for a single day.
     */
    public static DateRange ofSingleDay(LocalDate date) {
        return new DateRange(date, date);
    }

    /**
     * Creates a date range with the specified number of days before and after the center date.
     */
    public static DateRange ofDaysAround(LocalDate centerDate, int daysBefore, int daysAfter) {
        return new DateRange(
            centerDate.minusDays(daysBefore),
            centerDate.plusDays(daysAfter)
        );
    }
}