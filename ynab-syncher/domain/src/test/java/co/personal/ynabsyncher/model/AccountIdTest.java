package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

class AccountIdTest {

    @Nested
    class Construction {
        
        @Test
        void shouldCreateValidAccountId() {
            AccountId accountId = new AccountId("account-123");
            
            assertThat(accountId.value()).isEqualTo("account-123");
        }
        
        @Test
        void shouldCreateAccountIdWithSpecialCharacters() {
            AccountId accountId = new AccountId("account_123-ABC.xyz");
            
            assertThat(accountId.value()).isEqualTo("account_123-ABC.xyz");
        }
        
        @Test
        void shouldCreateAccountIdWithNumericValue() {
            AccountId accountId = new AccountId("123456789");
            
            assertThat(accountId.value()).isEqualTo("123456789");
        }
        
        @Test
        void shouldCreateAccountIdWithSingleCharacter() {
            AccountId accountId = new AccountId("A");
            
            assertThat(accountId.value()).isEqualTo("A");
        }
        
        @Test
        void shouldThrowWhenValueIsNull() {
            assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Account ID value cannot be null");
        }
        
        @Test
        void shouldThrowWhenValueIsEmpty() {
            assertThatThrownBy(() -> new AccountId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account ID value cannot be empty");
        }
        
        @Test
        void shouldThrowWhenValueIsBlank() {
            assertThatThrownBy(() -> new AccountId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account ID value cannot be empty");
        }
        
        @Test
        void shouldThrowWhenValueIsOnlyWhitespace() {
            assertThatThrownBy(() -> new AccountId("\t\n\r "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account ID value cannot be empty");
        }
        
        @Test
        void shouldAcceptValueWithLeadingAndTrailingSpaces() {
            // The validation trims but stores the original value
            AccountId accountId = new AccountId(" account-123 ");
            
            assertThat(accountId.value()).isEqualTo(" account-123 ");
        }
    }

    @Nested
    class FactoryMethod {
        
        @Test
        void shouldCreateAccountIdUsingOf() {
            AccountId accountId = AccountId.of("test-account");
            
            assertThat(accountId.value()).isEqualTo("test-account");
        }
        
        @Test
        void shouldThrowWhenOfParameterIsNull() {
            assertThatThrownBy(() -> AccountId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Account ID value cannot be null");
        }
        
        @Test
        void shouldThrowWhenOfParameterIsEmpty() {
            assertThatThrownBy(() -> AccountId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account ID value cannot be empty");
        }
        
        @Test
        void shouldCreateSameResultForConstructorAndFactory() {
            String value = "same-account-id";
            AccountId fromConstructor = new AccountId(value);
            AccountId fromFactory = AccountId.of(value);
            
            assertThat(fromConstructor).isEqualTo(fromFactory);
            assertThat(fromConstructor.value()).isEqualTo(fromFactory.value());
        }
    }

    @Nested
    class Equality {
        
        @Test
        void shouldBeEqualWhenValuesMatch() {
            AccountId accountId1 = AccountId.of("same-account");
            AccountId accountId2 = AccountId.of("same-account");
            
            assertThat(accountId1).isEqualTo(accountId2);
            assertThat(accountId1.hashCode()).isEqualTo(accountId2.hashCode());
        }
        
        @Test
        void shouldNotBeEqualWhenValuesDiffer() {
            AccountId accountId1 = AccountId.of("account-1");
            AccountId accountId2 = AccountId.of("account-2");
            
            assertThat(accountId1).isNotEqualTo(accountId2);
        }
        
        @Test
        void shouldBeEqualToItself() {
            AccountId accountId = AccountId.of("self-test");
            
            assertThat(accountId).isEqualTo(accountId);
        }
        
        @Test
        void shouldNotBeEqualToNull() {
            AccountId accountId = AccountId.of("not-null");
            
            assertThat(accountId).isNotEqualTo(null);
        }
        
        @Test
        void shouldNotBeEqualToDifferentType() {
            AccountId accountId = AccountId.of("account");
            String string = "account";
            
            assertThat(accountId).isNotEqualTo(string);
        }
        
        @Test
        void shouldBeEqualWhenValuesAreCaseSensitive() {
            AccountId accountId1 = AccountId.of("Account");
            AccountId accountId2 = AccountId.of("account");
            
            assertThat(accountId1).isNotEqualTo(accountId2);
        }
        
        @Test
        void shouldHaveConsistentHashCode() {
            AccountId accountId = AccountId.of("hash-test");
            int hash1 = accountId.hashCode();
            int hash2 = accountId.hashCode();
            
            assertThat(hash1).isEqualTo(hash2);
        }
        
        @Test
        void shouldHaveDifferentHashCodesForDifferentValues() {
            AccountId accountId1 = AccountId.of("account-1");
            AccountId accountId2 = AccountId.of("account-2");
            
            // Note: Hash codes could theoretically be equal, but very unlikely for different strings
            assertThat(accountId1.hashCode()).isNotEqualTo(accountId2.hashCode());
        }
    }

    @Nested
    class StringRepresentation {
        
        @Test
        void shouldReturnValueAsToString() {
            AccountId accountId = AccountId.of("my-account-id");
            
            assertThat(accountId.toString()).isEqualTo("my-account-id");
        }
        
        @Test
        void shouldReturnValueWithSpecialCharactersAsToString() {
            AccountId accountId = AccountId.of("account_123-ABC.xyz@domain");
            
            assertThat(accountId.toString()).isEqualTo("account_123-ABC.xyz@domain");
        }
        
        @Test
        void shouldReturnEmptyLookingValueAsToString() {
            AccountId accountId = AccountId.of("   spaces   ");
            
            assertThat(accountId.toString()).isEqualTo("   spaces   ");
        }
        
        @Test
        void shouldReturnSingleCharacterAsToString() {
            AccountId accountId = AccountId.of("X");
            
            assertThat(accountId.toString()).isEqualTo("X");
        }
    }

    @Nested
    class EdgeCases {
        
        @Test
        void shouldHandleVeryLongAccountId() {
            String longValue = "a".repeat(1000);
            AccountId accountId = AccountId.of(longValue);
            
            assertThat(accountId.value()).isEqualTo(longValue);
            assertThat(accountId.toString()).isEqualTo(longValue);
        }
        
        @Test
        void shouldHandleUnicodeCharacters() {
            AccountId accountId = AccountId.of("账户-123-éñ-🏦");
            
            assertThat(accountId.value()).isEqualTo("账户-123-éñ-🏦");
            assertThat(accountId.toString()).isEqualTo("账户-123-éñ-🏦");
        }
        
        @Test
        void shouldHandleNewlineCharacters() {
            AccountId accountId = AccountId.of("account\nwith\nnewlines");
            
            assertThat(accountId.value()).isEqualTo("account\nwith\nnewlines");
        }
        
        @Test
        void shouldHandleTabCharacters() {
            AccountId accountId = AccountId.of("account\twith\ttabs");
            
            assertThat(accountId.value()).isEqualTo("account\twith\ttabs");
        }
        
        @Test
        void shouldHandleJsonSpecialCharacters() {
            AccountId accountId = AccountId.of("account\"with'quotes{and}brackets[123]");
            
            assertThat(accountId.value()).isEqualTo("account\"with'quotes{and}brackets[123]");
        }
        
        @Test
        void shouldHandleSpacesButNotEmpty() {
            // Spaces at ends should be preserved, but the validation checks if trim() is empty
            AccountId accountId = AccountId.of(" valid ");
            
            assertThat(accountId.value()).isEqualTo(" valid ");
        }
    }

    @Nested
    class BusinessScenarios {
        
        @Test
        void shouldCreateYnabStyleAccountId() {
            AccountId accountId = AccountId.of("550e8400-e29b-41d4-a716-446655440000");
            
            assertThat(accountId.value()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }
        
        @Test
        void shouldCreateBankStyleAccountId() {
            AccountId accountId = AccountId.of("CHK_001_SAVINGS_2024");
            
            assertThat(accountId.value()).isEqualTo("CHK_001_SAVINGS_2024");
        }
        
        @Test
        void shouldCreateNumericBankAccountId() {
            AccountId accountId = AccountId.of("1234567890123456");
            
            assertThat(accountId.value()).isEqualTo("1234567890123456");
        }
        
        @Test
        void shouldCreateCustomAccountId() {
            AccountId accountId = AccountId.of("MyPersonalAccount2024");
            
            assertThat(accountId.value()).isEqualTo("MyPersonalAccount2024");
        }
    }
}