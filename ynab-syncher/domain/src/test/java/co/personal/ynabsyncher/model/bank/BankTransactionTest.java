package co.personal.ynabsyncher.model.bank;

import co.personal.ynabsyncher.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static co.personal.ynabsyncher.model.BankTransactionTestBuilder.aBankTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BankTransaction")
class BankTransactionTest {

    @Nested
    @DisplayName("Construction")
    class Construction {
        
        @Test
        @DisplayName("should create valid bank transaction")
        void shouldCreateValidBankTransaction() {
            var transaction = aBankTransaction()
                .withId("bank-1")
                .withAccountId("acc-1")
                .withAmount(100.50)
                .withDescription("Coffee Shop")
                .withMerchantName("Starbucks")
                .build();
            
            assertThat(transaction.id()).isEqualTo(TransactionId.of("bank-1"));
            assertThat(transaction.accountId()).isEqualTo(AccountId.of("acc-1"));
            assertThat(transaction.amount()).isEqualTo(Money.of(100.50));
            assertThat(transaction.description()).isEqualTo("Coffee Shop");
            assertThat(transaction.merchantName()).isEqualTo("Starbucks");
        }
        
        @Test
        @DisplayName("should require non-null transaction ID")
        void shouldRequireNonNullTransactionId() {
            assertThatThrownBy(() -> new BankTransaction(
                null, AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                "description", "merchant", "memo", "DEBIT", "ref", Category.unknown()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Transaction ID cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null account ID")
        void shouldRequireNonNullAccountId() {
            assertThatThrownBy(() -> new BankTransaction(
                TransactionId.of("tx-1"), null, LocalDate.now(), Money.of(100),
                "description", "merchant", "memo", "DEBIT", "ref", Category.unknown()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Account ID cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null date")
        void shouldRequireNonNullDate() {
            assertThatThrownBy(() -> new BankTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), null, Money.of(100),
                "description", "merchant", "memo", "DEBIT", "ref", Category.unknown()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Date cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null amount")
        void shouldRequireNonNullAmount() {
            assertThatThrownBy(() -> new BankTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), null,
                "description", "merchant", "memo", "DEBIT", "ref", Category.unknown()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Amount cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null description")
        void shouldRequireNonNullDescription() {
            assertThatThrownBy(() -> new BankTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                null, "merchant", "memo", "DEBIT", "ref", Category.unknown()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Description cannot be null");
        }
        
        @Test
        @DisplayName("should require non-null inferred category")
        void shouldRequireNonNullInferredCategory() {
            assertThatThrownBy(() -> new BankTransaction(
                TransactionId.of("tx-1"), AccountId.of("acc-1"), LocalDate.now(), Money.of(100),
                "description", "merchant", "memo", "DEBIT", "ref", null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("Inferred category cannot be null");
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        @DisplayName("should create transaction with unknown category")
        void shouldCreateTransactionWithUnknownCategory() {
            var transaction = BankTransaction.withUnknownCategory(
                TransactionId.of("tx-1"),
                AccountId.of("acc-1"),
                LocalDate.of(2024, 1, 15),
                Money.of(50.0),
                "Gas Station",
                "Shell",
                "Fuel purchase",
                "DEBIT",
                "REF123"
            );
            
            assertThat(transaction.inferredCategory()).isEqualTo(Category.unknown());
            assertThat(transaction.description()).isEqualTo("Gas Station");
            assertThat(transaction.merchantName()).isEqualTo("Shell");
        }
        
        @Test
        @DisplayName("should create new transaction with inferred category")
        void shouldCreateNewTransactionWithInferredCategory() {
            var original = aBankTransaction()
                .withDescription("Gas Station")
                .withInferredCategory(Category.unknown())
                .build();
            
            var category = Category.inferredCategory("Transportation");
            var updated = original.withInferredCategory(category);
            
            assertThat(updated.inferredCategory()).isEqualTo(category);
            assertThat(updated.description()).isEqualTo("Gas Station");
            assertThat(updated).isNotSameAs(original);
        }
    }
    
    @Nested
    @DisplayName("Business Logic")
    class BusinessLogic {
        
        @Test
        @DisplayName("should return merchant name as display name when available")
        void shouldReturnMerchantNameAsDisplayNameWhenAvailable() {
            var transaction = aBankTransaction()
                .withDescription("PURCHASE 123456")
                .withMerchantName("Starbucks Coffee")
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("Starbucks Coffee");
        }
        
        @Test
        @DisplayName("should return description as display name when merchant name is blank")
        void shouldReturnDescriptionAsDisplayNameWhenMerchantNameIsBlank() {
            var transaction = aBankTransaction()
                .withDescription("Online Purchase")
                .withMerchantName("")
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("Online Purchase");
        }
        
        @Test
        @DisplayName("should return description as display name when merchant name is null")
        void shouldReturnDescriptionAsDisplayNameWhenMerchantNameIsNull() {
            var transaction = aBankTransaction()
                .withDescription("ATM Withdrawal")
                .withMerchantName(null)
                .build();
            
            assertThat(transaction.displayName()).isEqualTo("ATM Withdrawal");
        }
        
        @Test
        @DisplayName("should identify debit transaction by negative amount")
        void shouldIdentifyDebitTransactionByNegativeAmount() {
            var transaction = aBankTransaction()
                .withAmount(-50.0)
                .withTransactionType("PURCHASE")
                .build();
            
            assertThat(transaction.isDebit()).isTrue();
        }
        
        @Test
        @DisplayName("should identify debit transaction by type")
        void shouldIdentifyDebitTransactionByType() {
            var transaction = aBankTransaction()
                .withAmount(50.0)
                .withTransactionType("DEBIT")
                .build();
            
            assertThat(transaction.isDebit()).isTrue();
        }
        
        @Test
        @DisplayName("should identify credit transaction")
        void shouldIdentifyCredtTransaction() {
            var transaction = aBankTransaction()
                .withAmount(100.0)
                .withTransactionType("CREDIT")
                .build();
            
            assertThat(transaction.isDebit()).isFalse();
        }
        
        @Test
        @DisplayName("should detect inferred category")
        void shouldDetectInferredCategory() {
            var withInferred = aBankTransaction()
                .withInferredCategory(Category.inferredCategory("Groceries"))
                .build();
            
            var withUnknown = aBankTransaction()
                .withInferredCategory(Category.unknown())
                .build();
            
            assertThat(withInferred.hasCategoryInferred()).isTrue();
            assertThat(withUnknown.hasCategoryInferred()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Equality")
    class Equality {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            var transaction1 = aBankTransaction()
                .withId("tx-1")
                .withAmount(100.0)
                .withDescription("Test")
                .build();
            
            var transaction2 = aBankTransaction()
                .withId("tx-1")
                .withAmount(100.0)
                .withDescription("Test")
                .build();
            
            assertThat(transaction1).isEqualTo(transaction2);
            assertThat(transaction1.hashCode()).isEqualTo(transaction2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when IDs differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            var transaction1 = aBankTransaction().withId("tx-1").build();
            var transaction2 = aBankTransaction().withId("tx-2").build();
            
            assertThat(transaction1).isNotEqualTo(transaction2);
        }
        
        @Test
        @DisplayName("should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            var transaction1 = aBankTransaction().withAmount(100.0).build();
            var transaction2 = aBankTransaction().withAmount(200.0).build();
            
            assertThat(transaction1).isNotEqualTo(transaction2);
        }
    }
    
    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {
        
        @Test
        @DisplayName("should include key fields in toString")
        void shouldIncludeKeyFieldsInToString() {
            var transaction = aBankTransaction()
                .withId("tx-1")
                .withAmount(100.0)
                .withDescription("Coffee")
                .withMerchantName("Starbucks")
                .build();
            
            var toString = transaction.toString();
            
            assertThat(toString).contains("tx-1");
            assertThat(toString).contains("100.0");
            assertThat(toString).contains("Coffee");
            assertThat(toString).contains("Starbucks");
        }
    }
}
