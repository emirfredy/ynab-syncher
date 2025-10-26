package co.personal.ynabsyncher.model.ynab;

import co.personal.ynabsyncher.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static co.personal.ynabsyncher.model.YnabTransactionTestBuilder.aYnabTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("YnabTransaction")
class YnabTransactionTest {

    @Nested
    @DisplayName("Construction")
    class Construction {
        
        @Test
        @DisplayName("should create valid YNAB transaction")
        void shouldCreateValidYnabTransaction() {
            var transaction = aYnabTransaction()
                .withId("ynab-1")
                .withAccountId("acc-1")
                .withAmount(75.25)
                .withPayeeName("Grocery Store")
                .withCategory(Category.ynabCategory("cat1", "Groceries"))
                .withClearedStatus(ClearedStatus.CLEARED)
                .build();
            
            assertThat(transaction.id()).isEqualTo(TransactionId.of("ynab-1"));
            assertThat(transaction.accountId()).isEqualTo(AccountId.of("acc-1"));
            assertThat(transaction.amount()).isEqualTo(Money.of(75.25));
            assertThat(transaction.payeeName()).isEqualTo("Grocery Store");
            assertThat(transaction.category().name()).isEqualTo("Groceries");
            assertThat(transaction.clearedStatus()).isEqualTo(ClearedStatus.CLEARED);
        }
        
        @Test
        @DisplayName("should require non-null transaction ID")
        void shouldRequireNonNullTransactionId() {
            assertThatThrownBy(() -> new YnabTransaction(
                null, AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                "payee", "memo", Category.ynabCategory("cat1", "Test"), 
                ClearedStatus.CLEARED, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Transaction ID cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null account ID")
        void shouldRequireNonNullAccountId() {
            assertThatThrownBy(() -> new YnabTransaction(
                TransactionId.of("tx-1"), null, LocalDate.now(), Money.of(100),
                "payee", "memo", Category.ynabCategory("cat1", "Test"), 
                ClearedStatus.CLEARED, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Account ID cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null date")
        void shouldRequireNonNullDate() {
            assertThatThrownBy(() -> new YnabTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), null, Money.of(100),
                "payee", "memo", Category.ynabCategory("cat1", "Test"), 
                ClearedStatus.CLEARED, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Date cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null amount")
        void shouldRequireNonNullAmount() {
            assertThatThrownBy(() -> new YnabTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), null,
                "payee", "memo", Category.ynabCategory("cat1", "Test"), 
                ClearedStatus.CLEARED, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Amount cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null category")
        void shouldRequireNonNullCategory() {
            assertThatThrownBy(() -> new YnabTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                "payee", "memo", null, ClearedStatus.CLEARED, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Category cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null cleared status")
        void shouldRequireNonNullClearedStatus() {
            assertThatThrownBy(() -> new YnabTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                "payee", "memo", Category.ynabCategory("cat1", "Test"), null, true, null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Cleared status cannot be null");
        }
    }
    
    @Nested
    @DisplayName("Business Logic")
    class BusinessLogic {
        
        @Test
        @DisplayName("should detect reconciled transaction")
        void shouldDetectReconciledTransaction() {
            var reconciled = aYnabTransaction()
                .withClearedStatus(ClearedStatus.RECONCILED)
                .build();
            
            var unreconciled = aYnabTransaction()
                .withClearedStatus(ClearedStatus.CLEARED)
                .build();
            
            assertThat(reconciled.isReconciled()).isTrue();
            assertThat(unreconciled.isReconciled()).isFalse();
        }
        
        @Test
        @DisplayName("should return payee name as display name when available")
        void shouldReturnPayeeNameAsDisplayNameWhenAvailable() {
            var transaction = aYnabTransaction()
                .withPayeeName("Amazon")
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("Amazon");
        }
        
        @Test
        @DisplayName("should return fallback display name when payee is null")
        void shouldReturnFallbackDisplayNameWhenPayeeIsNull() {
            var transaction = aYnabTransaction()
                .withPayeeName(null)
                .withCategory(Category.ynabCategory("cat1", "Shopping"))
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("Unknown Payee");
        }
        
        @Test
        @DisplayName("should return fallback display name when payee is blank")
        void shouldReturnFallbackDisplayNameWhenPayeeIsBlank() {
            var transaction = aYnabTransaction()
                .withPayeeName("")
                .withCategory(Category.ynabCategory("cat1", "Gas"))
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("Unknown Payee");
        }
    }
    
    @Nested
    @DisplayName("Optional Fields")
    class OptionalFields {
        
        @Test
        @DisplayName("should handle null memo")
        void shouldHandleNullMemo() {
            var transaction = aYnabTransaction()
                .withMemo(null)
                .build();
            
            assertThat(transaction.memo()).isNull();
        }
        
        @Test
        @DisplayName("should handle null flag color")
        void shouldHandleNullFlagColor() {
            var transaction = aYnabTransaction()
                .withFlagColor(null)
                .build();
            
            assertThat(transaction.flagColor()).isNull();
        }
        
        @Test
        @DisplayName("should handle approval status")
        void shouldHandleApprovalStatus() {
            var approved = aYnabTransaction()
                .withApproved(true)
                .build();
            
            var pending = aYnabTransaction()
                .withApproved(false)
                .build();
            
            assertThat(approved.approved()).isTrue();
            assertThat(pending.approved()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Equality")
    class Equality {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            var transaction1 = aYnabTransaction()
                .withId("tx-1")
                .withAmount(100.0)
                .withPayeeName("Test Store")
                .build();
            
            var transaction2 = aYnabTransaction()
                .withId("tx-1")
                .withAmount(100.0)
                .withPayeeName("Test Store")
                .build();
            
            assertThat(transaction1).isEqualTo(transaction2);
            assertThat(transaction1.hashCode()).isEqualTo(transaction2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when IDs differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            var transaction1 = aYnabTransaction().withId("tx-1").build();
            var transaction2 = aYnabTransaction().withId("tx-2").build();
            
            assertThat(transaction1).isNotEqualTo(transaction2);
        }
        
        @Test
        @DisplayName("should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            var transaction1 = aYnabTransaction().withAmount(100.0).build();
            var transaction2 = aYnabTransaction().withAmount(200.0).build();
            
            assertThat(transaction1).isNotEqualTo(transaction2);
        }
        
        @Test
        @DisplayName("should not be equal when cleared status differs")
        void shouldNotBeEqualWhenClearedStatusDiffers() {
            var cleared = aYnabTransaction().withClearedStatus(ClearedStatus.CLEARED).build();
            var uncleared = aYnabTransaction().withClearedStatus(ClearedStatus.UNCLEARED).build();
            
            assertThat(cleared).isNotEqualTo(uncleared);
        }
    }
    
    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {
        
        @Test
        @DisplayName("should include key fields in toString")
        void shouldIncludeKeyFieldsInToString() {
            var transaction = aYnabTransaction()
                .withId("ynab-1")
                .withAmount(50.0)
                .withPayeeName("Coffee Shop")
                .withClearedStatus(ClearedStatus.CLEARED)
                .build();
            
            var toString = transaction.toString();
            
            assertThat(toString).contains("ynab-1");
            assertThat(toString).contains("50.0");
            assertThat(toString).contains("Coffee Shop");
            assertThat(toString).contains("CLEARED");
        }
    }
}
