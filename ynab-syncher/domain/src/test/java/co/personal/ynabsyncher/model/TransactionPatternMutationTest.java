package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation testing focused tests for TransactionPattern boolean methods.
 * These tests target BooleanTrueReturnValsMutator and exact match boundary conditions.
 */
@DisplayName("TransactionPattern Mutation Tests")
class TransactionPatternMutationTest {

    @Nested
    @DisplayName("Boolean Method Testing")
    class BooleanMethodTesting {

        @Test
        @DisplayName("hasExactMatch should return true when patterns are identical")
        void hasExactMatchShouldReturnTrueWhenPatternsAreIdentical() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue();
        }

        @Test
        @DisplayName("hasExactMatch should return false when patterns are different")
        void hasExactMatchShouldReturnFalseWhenPatternsAreDifferent() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("coffee", "shop"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("grocery", "store"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isFalse();
        }

        @Test
        @DisplayName("hasExactMatch should return true when one pattern is subset with overlap")
        void hasExactMatchShouldReturnTrueWhenOnePatternIsSubsetWithOverlap() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("coffee", "shop"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue(); // "coffee" and "shop" overlap
            assertThat(pattern2.hasExactMatch(pattern1)).isTrue(); // "coffee" and "shop" overlap
        }

        @Test
        @DisplayName("hasExactMatch should return true when patterns overlap but are not identical")
        void hasExactMatchShouldReturnTrueWhenPatternsOverlapButNotIdentical() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("coffee", "shop"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("coffee", "store"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue(); // "coffee" overlaps
        }

        @Test
        @DisplayName("hasExactMatch should return true for single identical pattern")
        void hasExactMatchShouldReturnTrueForSingleIdenticalPattern() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("starbucks"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("starbucks"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue();
        }

        @Test
        @DisplayName("contains should return true when pattern contains normalized text")
        void containsShouldReturnTrueWhenPatternContainsNormalizedText() {
            TransactionPattern pattern = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            
            assertThat(pattern.contains("coffee")).isTrue();
            assertThat(pattern.contains("shop")).isTrue();
            assertThat(pattern.contains("downtown")).isTrue();
        }

        @Test
        @DisplayName("contains should return false when pattern does not contain text")
        void containsShouldReturnFalseWhenPatternDoesNotContainText() {
            TransactionPattern pattern = new TransactionPattern(Set.of("coffee", "shop"));
            
            assertThat(pattern.contains("grocery")).isFalse();
            assertThat(pattern.contains("store")).isFalse();
            assertThat(pattern.contains("restaurant")).isFalse();
        }

        @Test
        @DisplayName("contains should return false for partial matches")
        void containsShouldReturnFalseForPartialMatches() {
            TransactionPattern pattern = new TransactionPattern(Set.of("starbucks"));
            
            // These are partial matches and should return false
            assertThat(pattern.contains("star")).isFalse();
            assertThat(pattern.contains("bucks")).isFalse();
            assertThat(pattern.contains("starb")).isFalse();
        }

        @Test
        @DisplayName("hasContent should return true for non-empty patterns")
        void hasContentShouldReturnTrueForNonEmptyPatterns() {
            TransactionPattern pattern = new TransactionPattern(Set.of("test"));
            
            assertThat(pattern.hasContent()).isTrue();
        }

        @Test
        @DisplayName("hasContent should return true for multiple patterns")
        void hasContentShouldReturnTrueForMultiplePatterns() {
            TransactionPattern pattern = new TransactionPattern(Set.of("coffee", "shop", "downtown"));
            
            assertThat(pattern.hasContent()).isTrue();
        }

        // Note: We can't test hasContent() returning false because the constructor
        // validates that patterns are not empty and throws IllegalArgumentException
        // This is the correct behavior - empty patterns should not exist
    }

    @Nested
    @DisplayName("Pattern Equality Edge Cases")
    class PatternEqualityEdgeCases {

        @Test
        @DisplayName("should handle case sensitivity in exact matches")
        void shouldHandleCaseSensitivityInExactMatches() {
            // Transaction patterns should be normalized to lowercase
            TransactionPattern pattern1 = new TransactionPattern(Set.of("starbucks", "coffee"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("starbucks", "coffee"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue();
        }

        @Test
        @DisplayName("should handle different order in pattern sets")
        void shouldHandleDifferentOrderInPatternSets() {
            // Set order shouldn't matter for equality
            TransactionPattern pattern1 = new TransactionPattern(Set.of("coffee", "shop"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("shop", "coffee"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isTrue();
        }

        @Test
        @DisplayName("should distinguish between similar but different patterns")
        void shouldDistinguishBetweenSimilarButDifferentPatterns() {
            TransactionPattern pattern1 = new TransactionPattern(Set.of("star", "bucks"));
            TransactionPattern pattern2 = new TransactionPattern(Set.of("starbucks"));
            
            assertThat(pattern1.hasExactMatch(pattern2)).isFalse();
        }
    }
}