package co.personal.ynabsyncher.model;

import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents normalized text patterns extracted from bank transaction data.
 * Used for exact matching against learned category mappings.
 * 
 * Architecture: Simple dictionary approach - all text treated uniformly
 * for high-precision categorization without field semantics.
 */
public record TransactionPattern(
    Set<String> textPatterns
) {
    public TransactionPattern {
        Objects.requireNonNull(textPatterns, "Text patterns cannot be null");
        if (textPatterns.isEmpty()) {
            throw new IllegalArgumentException("Transaction pattern must have at least one text pattern");
        }
        
        // Validate all patterns are normalized
        textPatterns.forEach(pattern -> {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalArgumentException("Text patterns cannot contain null or blank values");
            }
        });
    }
    
    /**
     * Creates a transaction pattern from bank transaction data.
     * Extracts and normalizes all meaningful text for exact matching.
     */
    public static TransactionPattern fromBankTransaction(BankTransaction transaction) {
        Set<String> patterns = new HashSet<>();
        
        addNormalizedPattern(patterns, transaction.merchantName());
        addNormalizedPattern(patterns, transaction.description());
        
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("Transaction must have at least merchant name or description");
        }
        
        return new TransactionPattern(patterns);
    }
    
    /**
     * Checks if this pattern has any exact matches with another pattern.
     * Uses perfect text matching - no fuzzy/similarity algorithms.
     */
    public boolean hasExactMatch(TransactionPattern other) {
        return this.textPatterns.stream()
                .anyMatch(pattern -> other.textPatterns.contains(pattern));
    }
    
    /**
     * Checks if this pattern contains a specific normalized text.
     */
    public boolean contains(String normalizedText) {
        return textPatterns.contains(normalizedText);
    }
    
    /**
     * Returns the number of text patterns.
     */
    public int size() {
        return textPatterns.size();
    }
    
    /**
     * Checks if this pattern has meaningful content for matching.
     */
    public boolean hasContent() {
        return textPatterns.stream()
                .anyMatch(pattern -> pattern.length() >= 3);
    }
    
    /**
     * Adds normalized text to pattern set if meaningful.
     */
    private static void addNormalizedPattern(Set<String> patterns, String text) {
        String normalized = normalizeText(text);
        if (normalized != null && normalized.length() >= 3) {
            patterns.add(normalized);
        }
    }
    
    /**
     * Normalizes text for consistent exact matching.
     * Removes ambiguity while preserving meaningful content.
     */
    private static String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        return text.toLowerCase()
                   .trim()
                   .replaceAll("\\s+", " ")           // Normalize whitespace
                   .replaceAll("[^a-z0-9\\s]", "")    // Remove special characters
                   .trim();
    }
}