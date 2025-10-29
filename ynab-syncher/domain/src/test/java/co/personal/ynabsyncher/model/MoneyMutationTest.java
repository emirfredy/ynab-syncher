package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Additional edge case tests for Money class to improve mutation testing coverage.
 * These tests target specific boundary conditions and mathematical edge cases.
 */
@DisplayName("Money - Mutation Testing Edge Cases")
class MoneyMutationTest {

    @Nested
    @DisplayName("Boundary Value Testing")
    class BoundaryValueTesting {

        @Test
        @DisplayName("should handle exactly zero comparison boundaries")
        void shouldHandleExactlyZeroComparisonBoundaries() {
            Money zero = Money.zero();
            Money positiveOne = Money.ofMilliunits(1L);
            Money negativeOne = Money.ofMilliunits(-1L);

            // Test exact boundary conditions for isZero()
            assertThat(zero.isZero()).isTrue();
            assertThat(positiveOne.isZero()).isFalse();
            assertThat(negativeOne.isZero()).isFalse();

            // Test exact boundary conditions for isPositive()
            assertThat(zero.isPositive()).isFalse();  // Zero should NOT be positive
            assertThat(positiveOne.isPositive()).isTrue();
            assertThat(negativeOne.isPositive()).isFalse();

            // Test exact boundary conditions for isNegative()
            assertThat(zero.isNegative()).isFalse();  // Zero should NOT be negative
            assertThat(positiveOne.isNegative()).isFalse();
            assertThat(negativeOne.isNegative()).isTrue();
        }

        @Test
        @DisplayName("should handle boundary values around zero in arithmetic")
        void shouldHandleBoundaryValuesAroundZeroInArithmetic() {
            Money zero = Money.zero();
            Money one = Money.ofMilliunits(1L);
            Money minusOne = Money.ofMilliunits(-1L);

            // Adding zero should not change value
            assertThat(one.add(zero)).isEqualTo(one);
            assertThat(zero.add(one)).isEqualTo(one);

            // Subtracting zero should not change value
            assertThat(one.subtract(zero)).isEqualTo(one);
            
            // Subtracting from zero should negate
            assertThat(zero.subtract(one)).isEqualTo(minusOne);

            // Adding opposite values should result in zero
            assertThat(one.add(minusOne)).isEqualTo(zero);
            assertThat(minusOne.add(one)).isEqualTo(zero);
        }

        @ParameterizedTest
        @DisplayName("should handle boundary milliunits values")
        @ValueSource(longs = {Long.MIN_VALUE, -1000000L, -1L, 0L, 1L, 1000000L, Long.MAX_VALUE})
        void shouldHandleBoundaryMilliunitsValues(long milliunits) {
            Money money = Money.ofMilliunits(milliunits);

            assertThat(money.milliunits()).isEqualTo(milliunits);
            
            // Test state methods
            assertThat(money.isZero()).isEqualTo(milliunits == 0L);
            assertThat(money.isPositive()).isEqualTo(milliunits > 0L);
            assertThat(money.isNegative()).isEqualTo(milliunits < 0L);
            
            // Test that exactly one state is true (except for zero which has none true)
            int trueCount = 0;
            if (money.isZero()) trueCount++;
            if (money.isPositive()) trueCount++;
            if (money.isNegative()) trueCount++;
            
            assertThat(trueCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Comparison Boundary Testing")
    class ComparisonBoundaryTesting {

        @Test
        @DisplayName("should test all comparison boundaries with conditional mutations")
        void shouldTestAllComparisonBoundariesWithConditionalMutations() {
            Money zero = Money.zero();
            Money positive = Money.ofMilliunits(100L);
            Money negative = Money.ofMilliunits(-100L);

            // Test isZero with mutations: == 0 vs != 0, >= 0, <= 0
            assertThat(zero.isZero()).isTrue();
            assertThat(positive.isZero()).isFalse();
            assertThat(negative.isZero()).isFalse();

            // Test isPositive with mutations: > 0 vs >= 0, == 0, < 0, <= 0
            assertThat(zero.isPositive()).isFalse();
            assertThat(positive.isPositive()).isTrue();
            assertThat(negative.isPositive()).isFalse();

            // Test isNegative with mutations: < 0 vs <= 0, == 0, > 0, >= 0
            assertThat(zero.isNegative()).isFalse();
            assertThat(positive.isNegative()).isFalse();
            assertThat(negative.isNegative()).isTrue();
        }

        @Test
        @DisplayName("should test boundary values for greater than conditions")
        void shouldTestBoundaryValuesForGreaterThanConditions() {
            // Test values around zero to catch > vs >= mutations
            Money justAboveZero = Money.ofMilliunits(1L);
            Money justBelowZero = Money.ofMilliunits(-1L);
            Money zero = Money.zero();

            // isPositive uses milliunits > 0L
            assertThat(justAboveZero.isPositive()).isTrue();   // 1 > 0 = true
            assertThat(zero.isPositive()).isFalse();           // 0 > 0 = false
            assertThat(justBelowZero.isPositive()).isFalse();  // -1 > 0 = false
        }

        @Test
        @DisplayName("should test boundary values for less than conditions")
        void shouldTestBoundaryValuesForLessThanConditions() {
            // Test values around zero to catch < vs <= mutations
            Money justAboveZero = Money.ofMilliunits(1L);
            Money justBelowZero = Money.ofMilliunits(-1L);
            Money zero = Money.zero();

            // isNegative uses milliunits < 0L
            assertThat(justBelowZero.isNegative()).isTrue();   // -1 < 0 = true
            assertThat(zero.isNegative()).isFalse();           // 0 < 0 = false
            assertThat(justAboveZero.isNegative()).isFalse();  // 1 < 0 = false
        }
    }

    @Nested
    @DisplayName("Arithmetic Overflow Edge Cases")
    class ArithmeticOverflowEdgeCases {

        @Test
        @DisplayName("should handle addition near Long.MAX_VALUE")
        void shouldHandleAdditionNearLongMaxValue() {
            Money nearMax = Money.ofMilliunits(Long.MAX_VALUE - 1000L);
            Money small = Money.ofMilliunits(500L);

            Money result = nearMax.add(small);
            
            assertThat(result.milliunits()).isEqualTo(Long.MAX_VALUE - 500L);
            assertThat(result.isPositive()).isTrue();
        }

        @Test
        @DisplayName("should handle subtraction near Long.MIN_VALUE")
        void shouldHandleSubtractionNearLongMinValue() {
            Money nearMin = Money.ofMilliunits(Long.MIN_VALUE + 1000L);
            Money small = Money.ofMilliunits(500L);

            Money result = nearMin.subtract(small);
            
            assertThat(result.milliunits()).isEqualTo(Long.MIN_VALUE + 500L);
            assertThat(result.isNegative()).isTrue();
        }

        @Test
        @DisplayName("should handle subtraction that changes sign")
        void shouldHandleSubtractionThatChangesSign() {
            Money positive = Money.ofMilliunits(100L);
            Money larger = Money.ofMilliunits(200L);

            Money result = positive.subtract(larger);
            
            assertThat(result.milliunits()).isEqualTo(-100L);
            assertThat(result.isNegative()).isTrue();
            assertThat(result.isPositive()).isFalse();
            assertThat(result.isZero()).isFalse();
        }

        @Test
        @DisplayName("should handle addition that results in exactly zero")
        void shouldHandleAdditionThatResultsInExactlyZero() {
            Money positive = Money.ofMilliunits(100L);
            Money negative = Money.ofMilliunits(-100L);

            Money result = positive.add(negative);
            
            assertThat(result.milliunits()).isEqualTo(0L);
            assertThat(result.isZero()).isTrue();
            assertThat(result.isPositive()).isFalse();
            assertThat(result.isNegative()).isFalse();
        }
    }

    @Nested
    @DisplayName("Boolean Return Value Mutations")
    class BooleanReturnValueMutations {

        @Test
        @DisplayName("should verify boolean methods return correct primitive values")
        void shouldVerifyBooleanMethodsReturnCorrectPrimitiveValues() {
            Money zero = Money.zero();
            Money positive = Money.ofMilliunits(42L);
            Money negative = Money.ofMilliunits(-42L);

            // Test that true/false mutations are caught
            assertThat(zero.isZero()).isEqualTo(true);
            assertThat(positive.isZero()).isEqualTo(false);
            assertThat(negative.isZero()).isEqualTo(false);

            assertThat(zero.isPositive()).isEqualTo(false);
            assertThat(positive.isPositive()).isEqualTo(true);
            assertThat(negative.isPositive()).isEqualTo(false);

            assertThat(zero.isNegative()).isEqualTo(false);
            assertThat(positive.isNegative()).isEqualTo(false);
            assertThat(negative.isNegative()).isEqualTo(true);
        }

        @Test
        @DisplayName("should use boolean results in control flow to catch mutations")
        void shouldUseBooleanResultsInControlFlowToCatchMutations() {
            Money zero = Money.zero();
            Money positive = Money.ofMilliunits(100L);
            Money negative = Money.ofMilliunits(-100L);

            // Use boolean results in if conditions to ensure mutations are caught
            if (zero.isZero()) {
                assertThat(zero.milliunits()).isEqualTo(0L);
            } else {
                fail("Zero should be identified as zero");
            }

            if (positive.isPositive()) {
                assertThat(positive.milliunits()).isGreaterThan(0L);
            } else {
                fail("Positive money should be identified as positive");
            }

            if (negative.isNegative()) {
                assertThat(negative.milliunits()).isLessThan(0L);
            } else {
                fail("Negative money should be identified as negative");
            }

            // Test negated conditions too
            if (!positive.isZero()) {
                assertThat(positive.milliunits()).isNotEqualTo(0L);
            } else {
                fail("Positive money should not be zero");
            }

            if (!zero.isPositive()) {
                assertThat(zero.milliunits()).isLessThanOrEqualTo(0L);
            } else {
                fail("Zero should not be positive");
            }

            if (!zero.isNegative()) {
                assertThat(zero.milliunits()).isGreaterThanOrEqualTo(0L);
            } else {
                fail("Zero should not be negative");
            }
        }
    }
}