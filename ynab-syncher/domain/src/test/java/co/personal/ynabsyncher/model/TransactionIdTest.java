package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

class TransactionIdTest {

    @Nested
    class Construction {
        
        @Test
        void shouldCreateValidTransactionId() {
            TransactionId transactionId = new TransactionId("txn-123");
            
            assertThat(transactionId.value()).isEqualTo("txn-123");
        }
        
        @Test
        void shouldCreateTransactionIdWithSpecialCharacters() {
            TransactionId transactionId = new TransactionId("txn_123-ABC.xyz");
            
            assertThat(transactionId.value()).isEqualTo("txn_123-ABC.xyz");
        }
        
        @Test
        void shouldCreateTransactionIdWithNumericValue() {
            TransactionId transactionId = new TransactionId("987654321");
            
            assertThat(transactionId.value()).isEqualTo("987654321");
        }
        
        @Test
        void shouldCreateTransactionIdWithSingleCharacter() {
            TransactionId transactionId = new TransactionId("T");
            
            assertThat(transactionId.value()).isEqualTo("T");
        }
        
        @Test
        void shouldThrowWhenValueIsNull() {
            assertThatThrownBy(() -> new TransactionId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Transaction ID value cannot be null");
        }
        
        @Test
        void shouldThrowWhenValueIsEmpty() {
            assertThatThrownBy(() -> new TransactionId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID value cannot be empty");
        }
        
        @Test
        void shouldThrowWhenValueIsBlank() {
            assertThatThrownBy(() -> new TransactionId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID value cannot be empty");
        }
        
        @Test
        void shouldThrowWhenValueIsOnlyWhitespace() {
            assertThatThrownBy(() -> new TransactionId("\t\n\r "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID value cannot be empty");
        }
        
        @Test
        void shouldAcceptValueWithLeadingAndTrailingSpaces() {
            // The validation trims but stores the original value
            TransactionId transactionId = new TransactionId(" txn-123 ");
            
            assertThat(transactionId.value()).isEqualTo(" txn-123 ");
        }
    }

    @Nested
    class FactoryMethod {
        
        @Test
        void shouldCreateTransactionIdUsingOf() {
            TransactionId transactionId = TransactionId.of("test-transaction");
            
            assertThat(transactionId.value()).isEqualTo("test-transaction");
        }
        
        @Test
        void shouldThrowWhenOfParameterIsNull() {
            assertThatThrownBy(() -> TransactionId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Transaction ID value cannot be null");
        }
        
        @Test
        void shouldThrowWhenOfParameterIsEmpty() {
            assertThatThrownBy(() -> TransactionId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID value cannot be empty");
        }
        
        @Test
        void shouldCreateSameResultForConstructorAndFactory() {
            String value = "same-transaction-id";
            TransactionId fromConstructor = new TransactionId(value);
            TransactionId fromFactory = TransactionId.of(value);
            
            assertThat(fromConstructor).isEqualTo(fromFactory);
            assertThat(fromConstructor.value()).isEqualTo(fromFactory.value());
        }
    }

    @Nested
    class Equality {
        
        @Test
        void shouldBeEqualWhenValuesMatch() {
            TransactionId transactionId1 = TransactionId.of("same-transaction");
            TransactionId transactionId2 = TransactionId.of("same-transaction");
            
            assertThat(transactionId1).isEqualTo(transactionId2);
            assertThat(transactionId1.hashCode()).isEqualTo(transactionId2.hashCode());
        }
        
        @Test
        void shouldNotBeEqualWhenValuesDiffer() {
            TransactionId transactionId1 = TransactionId.of("transaction-1");
            TransactionId transactionId2 = TransactionId.of("transaction-2");
            
            assertThat(transactionId1).isNotEqualTo(transactionId2);
        }
        
        @Test
        void shouldBeEqualToItself() {
            TransactionId transactionId = TransactionId.of("self-test");
            
            assertThat(transactionId).isEqualTo(transactionId);
        }
        
        @Test
        void shouldNotBeEqualToNull() {
            TransactionId transactionId = TransactionId.of("not-null");
            
            assertThat(transactionId).isNotEqualTo(null);
        }
        
        @Test
        void shouldNotBeEqualToDifferentType() {
            TransactionId transactionId = TransactionId.of("transaction");
            String string = "transaction";
            
            assertThat(transactionId).isNotEqualTo(string);
        }
        
        @Test
        void shouldNotBeEqualToAccountIdWithSameValue() {
            TransactionId transactionId = TransactionId.of("same-value");
            AccountId accountId = AccountId.of("same-value");
            
            assertThat(transactionId).isNotEqualTo(accountId);
        }
        
        @Test
        void shouldBeEqualWhenValuesAreCaseSensitive() {
            TransactionId transactionId1 = TransactionId.of("Transaction");
            TransactionId transactionId2 = TransactionId.of("transaction");
            
            assertThat(transactionId1).isNotEqualTo(transactionId2);
        }
        
        @Test
        void shouldHaveConsistentHashCode() {
            TransactionId transactionId = TransactionId.of("hash-test");
            int hash1 = transactionId.hashCode();
            int hash2 = transactionId.hashCode();
            
            assertThat(hash1).isEqualTo(hash2);
        }
        
        @Test
        void shouldHaveDifferentHashCodesForDifferentValues() {
            TransactionId transactionId1 = TransactionId.of("transaction-1");
            TransactionId transactionId2 = TransactionId.of("transaction-2");
            
            // Note: Hash codes could theoretically be equal, but very unlikely for different strings
            assertThat(transactionId1.hashCode()).isNotEqualTo(transactionId2.hashCode());
        }
    }

    @Nested
    class StringRepresentation {
        
        @Test
        void shouldReturnValueAsToString() {
            TransactionId transactionId = TransactionId.of("my-transaction-id");
            
            assertThat(transactionId.toString()).isEqualTo("my-transaction-id");
        }
        
        @Test
        void shouldReturnValueWithSpecialCharactersAsToString() {
            TransactionId transactionId = TransactionId.of("txn_123-ABC.xyz@domain");
            
            assertThat(transactionId.toString()).isEqualTo("txn_123-ABC.xyz@domain");
        }
        
        @Test
        void shouldReturnEmptyLookingValueAsToString() {
            TransactionId transactionId = TransactionId.of("   spaces   ");
            
            assertThat(transactionId.toString()).isEqualTo("   spaces   ");
        }
        
        @Test
        void shouldReturnSingleCharacterAsToString() {
            TransactionId transactionId = TransactionId.of("X");
            
            assertThat(transactionId.toString()).isEqualTo("X");
        }
    }

    @Nested
    class EdgeCases {
        
        @Test
        void shouldHandleVeryLongTransactionId() {
            String longValue = "t".repeat(1000);
            TransactionId transactionId = TransactionId.of(longValue);
            
            assertThat(transactionId.value()).isEqualTo(longValue);
            assertThat(transactionId.toString()).isEqualTo(longValue);
        }
        
        @Test
        void shouldHandleUnicodeCharacters() {
            TransactionId transactionId = TransactionId.of("交易-123-éñ-💰");
            
            assertThat(transactionId.value()).isEqualTo("交易-123-éñ-💰");
            assertThat(transactionId.toString()).isEqualTo("交易-123-éñ-💰");
        }
        
        @Test
        void shouldHandleNewlineCharacters() {
            TransactionId transactionId = TransactionId.of("transaction\nwith\nnewlines");
            
            assertThat(transactionId.value()).isEqualTo("transaction\nwith\nnewlines");
        }
        
        @Test
        void shouldHandleTabCharacters() {
            TransactionId transactionId = TransactionId.of("transaction\twith\ttabs");
            
            assertThat(transactionId.value()).isEqualTo("transaction\twith\ttabs");
        }
        
        @Test
        void shouldHandleJsonSpecialCharacters() {
            TransactionId transactionId = TransactionId.of("txn\"with'quotes{and}brackets[123]");
            
            assertThat(transactionId.value()).isEqualTo("txn\"with'quotes{and}brackets[123]");
        }
        
        @Test
        void shouldHandleSpacesButNotEmpty() {
            // Spaces at ends should be preserved, but the validation checks if trim() is empty
            TransactionId transactionId = TransactionId.of(" valid ");
            
            assertThat(transactionId.value()).isEqualTo(" valid ");
        }
    }

    @Nested
    class BusinessScenarios {
        
        @Test
        void shouldCreateYnabStyleTransactionId() {
            TransactionId transactionId = TransactionId.of("550e8400-e29b-41d4-a716-446655440000");
            
            assertThat(transactionId.value()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }
        
        @Test
        void shouldCreateBankStyleTransactionId() {
            TransactionId transactionId = TransactionId.of("TXN_2024_10_24_001");
            
            assertThat(transactionId.value()).isEqualTo("TXN_2024_10_24_001");
        }
        
        @Test
        void shouldCreateSequentialTransactionId() {
            TransactionId transactionId = TransactionId.of("20241024000001");
            
            assertThat(transactionId.value()).isEqualTo("20241024000001");
        }
        
        @Test
        void shouldCreateCustomTransactionId() {
            TransactionId transactionId = TransactionId.of("GROCERY_STORE_2024");
            
            assertThat(transactionId.value()).isEqualTo("GROCERY_STORE_2024");
        }
        
        @Test
        void shouldCreateImportedTransactionId() {
            TransactionId transactionId = TransactionId.of("IMPORT_CSV_ROW_42");
            
            assertThat(transactionId.value()).isEqualTo("IMPORT_CSV_ROW_42");
        }
    }
}