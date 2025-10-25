package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Nested
    class Creation {
        
        @Test
        void shouldCreateFromBigDecimal() {
            Money money = Money.of(new BigDecimal("123.45"));
            
            assertThat(money.milliunits()).isEqualTo(123450L);
        }
        
        @Test
        void shouldCreateFromDouble() {
            Money money = Money.of(123.45);
            
            assertThat(money.milliunits()).isEqualTo(123450L);
        }
        
        @Test
        void shouldCreateFromMilliunits() {
            Money money = Money.ofMilliunits(123450L);
            
            assertThat(money.milliunits()).isEqualTo(123450L);
        }
        
        @Test
        void shouldCreateZeroMoney() {
            Money money = Money.zero();
            
            assertThat(money.milliunits()).isEqualTo(0L);
            assertThat(money.isZero()).isTrue();
        }
        
        @Test
        void shouldThrowWhenBigDecimalIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Amount cannot be null");
        }
        
        @Test
        void shouldHandleRounding() {
            // Test rounding up
            Money money1 = Money.of(new BigDecimal("123.456"));
            assertThat(money1.milliunits()).isEqualTo(123456L);
            
            // Test rounding to nearest
            Money money2 = Money.of(new BigDecimal("123.4555"));
            assertThat(money2.milliunits()).isEqualTo(123456L);
        }
    }

    @Nested
    class StateChecking {
        
        @Test
        void shouldCheckIfZero() {
            assertThat(Money.zero().isZero()).isTrue();
            assertThat(Money.ofMilliunits(0L).isZero()).isTrue();
            assertThat(Money.of(0.0).isZero()).isTrue();
            
            assertThat(Money.of(1.0).isZero()).isFalse();
            assertThat(Money.of(-1.0).isZero()).isFalse();
        }
        
        @Test
        void shouldCheckIfPositive() {
            assertThat(Money.of(100.50).isPositive()).isTrue();
            assertThat(Money.ofMilliunits(1L).isPositive()).isTrue();
            
            assertThat(Money.zero().isPositive()).isFalse();
            assertThat(Money.of(-100.50).isPositive()).isFalse();
        }
        
        @Test
        void shouldCheckIfNegative() {
            assertThat(Money.of(-100.50).isNegative()).isTrue();
            assertThat(Money.ofMilliunits(-1L).isNegative()).isTrue();
            
            assertThat(Money.zero().isNegative()).isFalse();
            assertThat(Money.of(100.50).isNegative()).isFalse();
        }
    }

    @Nested
    class Arithmetic {
        
        @Test
        void shouldAddMoney() {
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(23.25);
            
            Money result = money1.add(money2);
            
            assertThat(result.toDecimal()).isEqualByComparingTo(new BigDecimal("123.75"));
            assertThat(result.milliunits()).isEqualTo(123750L);
        }
        
        @Test
        void shouldSubtractMoney() {
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(23.25);
            
            Money result = money1.subtract(money2);
            
            assertThat(result.toDecimal()).isEqualByComparingTo(new BigDecimal("77.25"));
            assertThat(result.milliunits()).isEqualTo(77250L);
        }
        
        @Test
        void shouldHandleNegativeResults() {
            Money money1 = Money.of(50.00);
            Money money2 = Money.of(75.00);
            
            Money result = money1.subtract(money2);
            
            assertThat(result.isNegative()).isTrue();
            assertThat(result.toDecimal()).isEqualByComparingTo(new BigDecimal("-25.00"));
        }
        
        @Test
        void shouldThrowWhenAddingNull() {
            Money money = Money.of(100.00);
            
            assertThatThrownBy(() -> money.add(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Other money cannot be null");
        }
        
        @Test
        void shouldThrowWhenSubtractingNull() {
            Money money = Money.of(100.00);
            
            assertThatThrownBy(() -> money.subtract(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Other money cannot be null");
        }
    }

    @Nested
    class Conversion {
        
        @Test
        void shouldConvertToDecimal() {
            Money money = Money.ofMilliunits(123450L);
            
            BigDecimal decimal = money.toDecimal();
            
            assertThat(decimal).isEqualByComparingTo(new BigDecimal("123.45"));
        }
        
        @Test
        void shouldConvertZeroToDecimal() {
            Money money = Money.zero();
            
            BigDecimal decimal = money.toDecimal();
            
            assertThat(decimal).isEqualByComparingTo(BigDecimal.ZERO);
        }
        
        @Test
        void shouldConvertNegativeToDecimal() {
            Money money = Money.ofMilliunits(-123450L);
            
            BigDecimal decimal = money.toDecimal();
            
            assertThat(decimal).isEqualByComparingTo(new BigDecimal("-123.45"));
        }
        
        @Test
        void shouldConvertToStringViaToDeci‌mal() {
            Money money = Money.of(123.45);
            
            String result = money.toString();
            
            assertThat(result).isEqualTo("123.45");
        }
    }

    @Nested
    class EdgeCases {
        
        @Test
        void shouldHandleLargeAmounts() {
            Money money = Money.of(1_000_000.00);
            
            assertThat(money.isPositive()).isTrue();
            assertThat(money.milliunits()).isEqualTo(1_000_000_000L);
        }
        
        @Test
        void shouldHandleVerySmallAmounts() {
            Money money = Money.of(0.001);
            
            assertThat(money.milliunits()).isEqualTo(1L);
            assertThat(money.toDecimal()).isEqualByComparingTo(new BigDecimal("0.00"));
        }
        
        @Test
        void shouldHandlePrecisionLoss() {
            // Double precision can cause rounding issues
            Money money = Money.of(0.1 + 0.2); // Classic floating point issue
            
            // Should handle the precision correctly
            assertThat(money.toDecimal()).isEqualByComparingTo(new BigDecimal("0.30"));
        }
    }
}