package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.BankTransactionData;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.api.dto.ImportResult;
import co.personal.ynabsyncher.api.usecase.ImportBankTransactions;
import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.bank.BankTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Import Bank Transactions - Functional Tests")
class ImportBankTransactionsUseCaseFunctionalTest {

    private ImportBankTransactions importBankTransactions;

    @BeforeEach
    void setUp() {
        importBankTransactions = new ImportBankTransactionsUseCase();
    }

    @Nested
    @DisplayName("Single Transaction Import")
    class SingleTransactionImport {

        @Test
        @DisplayName("Should successfully import single valid transaction and return it")
        void shouldSuccessfullyImportSingleValidTransaction() {
            // Given
            String accountId = "acc-123";
            BankTransactionData transactionData = new BankTransactionData(
                "2024-01-15",
                "STARBUCKS COFFEE #1234",
                "-5.99",
                "Starbucks"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(transactionData)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then - Verify response statistics
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(1);
            assertThat(response.successfulImports()).isEqualTo(1);
            assertThat(response.duplicatesSkipped()).isEqualTo(0);
            assertThat(response.errors()).isEmpty();
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.successRate()).isEqualTo(1.0);

            // Then - Verify imported transactions are returned for other use cases
            assertThat(response.importedTransactions()).hasSize(1);
            
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            assertThat(importedTransaction.accountId()).isEqualTo(AccountId.of(accountId));
            assertThat(importedTransaction.date()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(importedTransaction.description()).isEqualTo("STARBUCKS COFFEE #1234");
            assertThat(importedTransaction.merchantName()).isEqualTo("Starbucks");
            assertThat(importedTransaction.amount()).isEqualTo(Money.of(new BigDecimal("-5.99")));
        }

        @Test
        @DisplayName("Should handle transaction with missing merchant name")
        void shouldHandleTransactionWithMissingMerchantName() {
            // Given
            String accountId = "acc-456";
            BankTransactionData transactionData = new BankTransactionData(
                "2024-01-20",
                "GROCERY STORE PURCHASE",
                "-125.50",
                null // No merchant name provided
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(transactionData)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(1);
            assertThat(response.errors()).isEmpty();

            // Verify transaction with description as merchant name
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            assertThat(importedTransaction.merchantName()).isEqualTo("GROCERY STORE PURCHASE");
        }
    }

    @Nested
    @DisplayName("Batch Transaction Import")
    class BatchTransactionImport {

        @Test
        @DisplayName("Should successfully import multiple valid transactions")
        void shouldSuccessfullyImportMultipleValidTransactions() {
            // Given
            String accountId = "acc-789";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "COFFEE SHOP", "-4.50", "Daily Grind"),
                new BankTransactionData("2024-01-11", "SALARY DEPOSIT", "2500.00", "ACME Corp"),
                new BankTransactionData("2024-01-12", "GROCERY SHOPPING", "-87.25", "SuperMart")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(3);
            assertThat(response.successfulImports()).isEqualTo(3);
            assertThat(response.duplicatesSkipped()).isEqualTo(0);
            assertThat(response.errors()).isEmpty();

            // Verify all transactions are returned
            assertThat(response.importedTransactions()).hasSize(3);
        }

        @Test
        @DisplayName("Should detect duplicates within batch and skip them")
        void shouldDetectDuplicatesWithinBatchAndSkipThem() {
            // Given
            String accountId = "acc-dup";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "COFFEE SHOP", "-4.50", "Daily Grind"),
                new BankTransactionData("2024-01-10", "COFFEE SHOP", "-4.50", "Daily Grind"), // Exact duplicate
                new BankTransactionData("2024-01-11", "DIFFERENT STORE", "-10.00", "Other Shop")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(3);
            assertThat(response.successfulImports()).isEqualTo(2); // Only 2 unique transactions
            assertThat(response.duplicatesSkipped()).isEqualTo(1);
            assertThat(response.errors()).isEmpty();

            // Verify only unique transactions are returned
            assertThat(response.importedTransactions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationErrorHandling {

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() {
            // Given
            String accountId = "acc-error";
            BankTransactionData invalidTransaction = new BankTransactionData(
                "invalid-date", // Invalid date format
                "SOME PURCHASE",
                "-10.00",
                "Some Store"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(invalidTransaction)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.FAILED);
            assertThat(response.successfulImports()).isEqualTo(0);
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0)).contains("Line 1:");
            
            // No transactions should be returned
            assertThat(response.importedTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Should handle invalid amount format")
        void shouldHandleInvalidAmountFormat() {
            // Given
            String accountId = "acc-error";
            BankTransactionData invalidTransaction = new BankTransactionData(
                "2024-01-15",
                "SOME PURCHASE",
                "not-a-number", // Invalid amount
                "Some Store"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(invalidTransaction)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.FAILED);
            assertThat(response.successfulImports()).isEqualTo(0);
            assertThat(response.errors()).hasSize(1);
            
            // No transactions should be returned
            assertThat(response.importedTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid transactions")
        void shouldHandleMixedValidAndInvalidTransactions() {
            // Given
            String accountId = "acc-mixed";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "VALID TRANSACTION", "-10.00", "Valid Store"),
                new BankTransactionData("invalid-date", "INVALID TRANSACTION", "-20.00", "Invalid Store"),
                new BankTransactionData("2024-01-12", "ANOTHER VALID", "-30.00", "Another Store")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.PARTIAL_SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(3);
            assertThat(response.successfulImports()).isEqualTo(2);
            assertThat(response.errors()).hasSize(1);
            
            // Only valid transactions should be returned
            assertThat(response.importedTransactions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Null Request Handling")
    class NullRequestHandling {

        @Test
        @DisplayName("Should throw exception for null request")
        void shouldThrowExceptionForNullRequest() {
            // When/Then
            assertThatThrownBy(() -> importBankTransactions.importTransactions(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Import request cannot be null");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle empty transaction list")
        void shouldHandleEmptyTransactionList() {
            // Given - Empty list validation happens at DTO boundary
            String accountId = "acc-empty";

            // When/Then - Should throw validation exception at DTO construction
            assertThatThrownBy(() -> new ImportBankTransactionsRequest(accountId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transactions list cannot be empty");
        }

        @Test
        @DisplayName("Should handle very long merchant name truncation")
        void shouldHandleVeryLongMerchantNameTruncation() {
            // Given
            String accountId = "acc-long";
            String longMerchantName = "A".repeat(60); // Exceeds MAX_MERCHANT_NAME_LENGTH (50)
            BankTransactionData transactionData = new BankTransactionData(
                "2024-01-15",
                "PURCHASE DESCRIPTION",
                "-10.00",
                longMerchantName
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(transactionData)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(1);
            
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            assertThat(importedTransaction.merchantName()).hasSize(50); // Truncated to exactly 50 chars
            assertThat(importedTransaction.merchantName()).isEqualTo("A".repeat(50));
        }

        @Test
        @DisplayName("Should handle very long description as merchant name when merchant is null")
        void shouldHandleVeryLongDescriptionAsMerchantNameWhenMerchantIsNull() {
            // Given
            String accountId = "acc-long-desc";
            String longDescription = "VERY LONG DESCRIPTION THAT EXCEEDS FIFTY CHARACTERS IN LENGTH FOR TESTING PURPOSES";
            BankTransactionData transactionData = new BankTransactionData(
                "2024-01-15",
                longDescription,
                "-10.00",
                null // No merchant provided, will use description
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(transactionData)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(1);
            
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            assertThat(importedTransaction.merchantName()).hasSize(50); // Truncated to exactly 50 chars
            assertThat(importedTransaction.description()).isEqualTo(longDescription); // Original description preserved
        }

        @Test
        @DisplayName("Should reject blank descriptions")
        void shouldRejectBlankDescriptions() {
            // Given - Blank description validation happens at DTO boundary

            // When/Then - Should throw validation exceptions at DTO construction
            assertThatThrownBy(() -> new BankTransactionData("2024-01-15", "", "-10.00", "Store"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot be blank");
                
            assertThatThrownBy(() -> new BankTransactionData("2024-01-16", "   ", "-20.00", "Store"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot be blank");
                
            assertThatThrownBy(() -> new BankTransactionData("2024-01-17", "\t\n", "-30.00", "Store"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot be blank");
        }

        @Test
        @DisplayName("Should handle maximum batch size efficiently")
        void shouldHandleMaximumBatchSizeEfficiently() {
            // Given
            String accountId = "acc-large-batch";
            List<BankTransactionData> largeBatch = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                largeBatch.add(new BankTransactionData(
                    "2024-01-15",
                    "TRANSACTION " + i,
                    "-" + i + ".00",
                    "Store " + i
                ));
            }
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, largeBatch);

            // When
            long startTime = System.currentTimeMillis();
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(1000);
            assertThat(response.successfulImports()).isEqualTo(1000);
            assertThat(response.duplicatesSkipped()).isEqualTo(0);
            assertThat(response.errors()).isEmpty();
            assertThat(response.importedTransactions()).hasSize(1000);
            
            // Performance check: should complete within reasonable time (2 seconds)
            assertThat(endTime - startTime).isLessThan(2000);
        }
    }

    @Nested
    @DisplayName("Advanced Duplicate Detection")
    class AdvancedDuplicateDetection {

        @Test
        @DisplayName("Should detect duplicates with different merchant names")
        void shouldDetectDuplicatesWithDifferentMerchantNames() {
            // Given - Same account, date, amount, description but different merchant
            // According to our hash logic: account + date + amount + description determines duplicates
            String accountId = "acc-dup-merchant";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "COFFEE PURCHASE", "-4.50", "Starbucks"),
                new BankTransactionData("2024-01-10", "COFFEE PURCHASE", "-4.50", "Daily Grind") // Same description, different merchant
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then - These should be treated as duplicates (same account+date+amount+description)
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(1); // Only first one imported
            assertThat(response.duplicatesSkipped()).isEqualTo(1); // Second one is duplicate
            assertThat(response.importedTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle case sensitivity in duplicate detection")
        void shouldHandleCaseSensitivityInDuplicateDetection() {
            // Given - Same transaction with different case
            String accountId = "acc-case";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "STARBUCKS COFFEE", "-4.50", "Starbucks"),
                new BankTransactionData("2024-01-10", "starbucks coffee", "-4.50", "starbucks") // Different case
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then - Case-sensitive comparison: should be treated as different transactions
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(2);
            assertThat(response.duplicatesSkipped()).isEqualTo(0);
            assertThat(response.importedTransactions()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle whitespace variations in descriptions")
        void shouldHandleWhitespaceVariationsInDescriptions() {
            // Given - Same transaction with different whitespace
            String accountId = "acc-whitespace";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "COFFEE SHOP", "-4.50", "Store"),
                new BankTransactionData("2024-01-10", "COFFEE  SHOP", "-4.50", "Store"),   // Extra space
                new BankTransactionData("2024-01-10", " COFFEE SHOP ", "-4.50", "Store")  // Leading/trailing spaces
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then - Whitespace differences: should be treated as different transactions
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            assertThat(response.duplicatesSkipped()).isEqualTo(0);
            assertThat(response.importedTransactions()).hasSize(3);
        }

        @Test
        @DisplayName("Should generate consistent hashes for identical transactions")
        void shouldGenerateConsistentHashesForIdenticalTransactions() {
            // Given - Multiple identical transactions in different positions
            String accountId = "acc-hash-consistency";
            BankTransactionData identicalTransaction = new BankTransactionData("2024-01-10", "STARBUCKS", "-4.50", "Coffee");
            List<BankTransactionData> transactions = List.of(
                identicalTransaction,
                new BankTransactionData("2024-01-11", "DIFFERENT", "-10.00", "Other"),
                identicalTransaction, // Identical to first
                new BankTransactionData("2024-01-12", "ANOTHER", "-20.00", "Another"),
                identicalTransaction  // Identical to first and third
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then - Only one instance of identical transaction should be imported
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(5);
            assertThat(response.successfulImports()).isEqualTo(3); // 3 unique transactions
            assertThat(response.duplicatesSkipped()).isEqualTo(2); // 2 duplicates skipped
            assertThat(response.importedTransactions()).hasSize(3);
        }

        @Test
        @DisplayName("Should handle complex duplicate scenarios")
        void shouldHandleComplexDuplicateScenarios() {
            // Given - Mix of unique, duplicate, and similar-but-different transactions
            String accountId = "acc-complex";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "COFFEE", "-4.50", "Starbucks"),     // Unique
                new BankTransactionData("2024-01-10", "COFFEE", "-4.50", "Starbucks"),     // Duplicate of #1
                new BankTransactionData("2024-01-10", "COFFEE", "-4.51", "Starbucks"),     // Different amount
                new BankTransactionData("2024-01-11", "COFFEE", "-4.50", "Starbucks"),     // Different date
                new BankTransactionData("2024-01-10", "COFFEE", "-4.50", "Starbucks"),     // Duplicate of #1
                new BankTransactionData("2024-01-10", "LUNCH", "-12.00", "Restaurant")     // Completely different
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(6);
            assertThat(response.successfulImports()).isEqualTo(4); // 4 unique transactions
            assertThat(response.duplicatesSkipped()).isEqualTo(2); // 2 exact duplicates
            assertThat(response.importedTransactions()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Money and Date Validation")
    class MoneyAndDateValidation {

        @Test
        @DisplayName("Should handle zero amounts")
        void shouldHandleZeroAmounts() {
            // Given
            String accountId = "acc-zero";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "ZERO AMOUNT", "0.00", "Store"),
                new BankTransactionData("2024-01-16", "NEGATIVE ZERO", "-0.00", "Store"),
                new BankTransactionData("2024-01-17", "JUST ZERO", "0", "Store")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            
            // Verify all zero amounts are normalized
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).amount()).isEqualTo(Money.of(BigDecimal.ZERO));
            assertThat(importedTransactions.get(1).amount()).isEqualTo(Money.of(BigDecimal.ZERO));
            assertThat(importedTransactions.get(2).amount()).isEqualTo(Money.of(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should handle very large amounts")
        void shouldHandleVeryLargeAmounts() {
            // Given
            String accountId = "acc-large";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "LARGE POSITIVE", "99999999.99", "Bank"),
                new BankTransactionData("2024-01-16", "LARGE NEGATIVE", "-99999999.99", "Bank")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(2);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).amount()).isEqualTo(Money.of(new BigDecimal("99999999.99")));
            assertThat(importedTransactions.get(1).amount()).isEqualTo(Money.of(new BigDecimal("-99999999.99")));
        }

        @Test
        @DisplayName("Should handle special dates")
        void shouldHandleSpecialDates() {
            // Given
            String accountId = "acc-dates";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-02-29", "LEAP YEAR", "-10.00", "Store"), // Leap year
                new BankTransactionData("2024-12-31", "YEAR END", "-20.00", "Store"),  // Year boundary
                new BankTransactionData("2024-01-01", "YEAR START", "-30.00", "Store"), // Year start
                new BankTransactionData("1970-01-01", "EPOCH", "-40.00", "Store")      // Unix epoch
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(4);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).date()).isEqualTo(LocalDate.of(2024, 2, 29));
            assertThat(importedTransactions.get(1).date()).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(importedTransactions.get(2).date()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(importedTransactions.get(3).date()).isEqualTo(LocalDate.of(1970, 1, 1));
        }

        @Test
        @DisplayName("Should preserve decimal precision")
        void shouldPreserveDecimalPrecision() {
            // Given
            String accountId = "acc-precision";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "HIGH PRECISION", "123.456789", "Store"),
                new BankTransactionData("2024-01-16", "MANY DECIMALS", "-0.123456", "Store"),
                new BankTransactionData("2024-01-17", "TRAILING ZEROS", "100.100", "Store")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).amount()).isEqualTo(Money.of(new BigDecimal("123.456789")));
            assertThat(importedTransactions.get(1).amount()).isEqualTo(Money.of(new BigDecimal("-0.123456")));
            assertThat(importedTransactions.get(2).amount()).isEqualTo(Money.of(new BigDecimal("100.100")));
        }

        @Test
        @DisplayName("Should handle invalid leap year dates")
        void shouldHandleInvalidLeapYearDates() {
            // Given
            String accountId = "acc-invalid-leap";
            BankTransactionData invalidLeapYear = new BankTransactionData(
                "2023-02-29", // 2023 is not a leap year
                "INVALID LEAP",
                "-10.00",
                "Store"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(invalidLeapYear)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.FAILED);
            assertThat(response.successfulImports()).isEqualTo(0);
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0)).contains("Line 1:");
            assertThat(response.importedTransactions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transaction Type Determination")
    class TransactionTypeDetermination {

        @Test
        @DisplayName("Should correctly identify debit transactions")
        void shouldCorrectlyIdentifyDebitTransactions() {
            // Given
            String accountId = "acc-debit";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "PURCHASE", "-10.50", "Store"),
                new BankTransactionData("2024-01-16", "FEE", "-5.00", "Bank"),
                new BankTransactionData("2024-01-17", "WITHDRAWAL", "-100.00", "ATM")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).transactionType()).isEqualTo("DEBIT");
            assertThat(importedTransactions.get(1).transactionType()).isEqualTo("DEBIT");
            assertThat(importedTransactions.get(2).transactionType()).isEqualTo("DEBIT");
        }

        @Test
        @DisplayName("Should correctly identify credit transactions")
        void shouldCorrectlyIdentifyCreditTransactions() {
            // Given
            String accountId = "acc-credit";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "SALARY", "2500.00", "Employer"),
                new BankTransactionData("2024-01-16", "REFUND", "25.99", "Store"),
                new BankTransactionData("2024-01-17", "INTEREST", "0.50", "Bank")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions.get(0).transactionType()).isEqualTo("CREDIT");
            assertThat(importedTransactions.get(1).transactionType()).isEqualTo("CREDIT");
            assertThat(importedTransactions.get(2).transactionType()).isEqualTo("CREDIT");
        }

        @Test
        @DisplayName("Should handle zero amount transaction type")
        void shouldHandleZeroAmountTransactionType() {
            // Given
            String accountId = "acc-zero-type";
            BankTransactionData zeroTransaction = new BankTransactionData(
                "2024-01-15",
                "ZERO AMOUNT",
                "0.00",
                "Bank"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(zeroTransaction)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(1);
            
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            assertThat(importedTransaction.transactionType()).isEqualTo("CREDIT"); // Zero is not negative, so CREDIT
        }
    }

    @Nested
    @DisplayName("Response Contract Validation")
    class ResponseContractValidation {

        @Test
        @DisplayName("Should ensure response statistics add up correctly")
        void shouldEnsureResponseStatisticsAddUpCorrectly() {
            // Given - Mix of valid, duplicate, and invalid transactions
            String accountId = "acc-stats";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-10", "VALID 1", "-10.00", "Store"),     // Valid
                new BankTransactionData("2024-01-10", "VALID 1", "-10.00", "Store"),     // Duplicate
                new BankTransactionData("2024-01-11", "VALID 2", "-20.00", "Store"),     // Valid
                new BankTransactionData("invalid-date", "INVALID", "-30.00", "Store"),   // Invalid date
                new BankTransactionData("2024-01-12", "VALID 3", "-40.00", "Store")      // Valid
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.PARTIAL_SUCCESS);
            assertThat(response.totalProcessed()).isEqualTo(5);
            assertThat(response.successfulImports()).isEqualTo(3); // 3 unique valid transactions
            assertThat(response.duplicatesSkipped()).isEqualTo(1); // 1 duplicate
            assertThat(response.errors()).hasSize(1); // 1 invalid date
            
            // Verify the math: totalProcessed = successfulImports + duplicatesSkipped + errorCount
            int expectedTotal = response.successfulImports() + response.duplicatesSkipped() + response.errors().size();
            assertThat(response.totalProcessed()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("Should maintain transaction order in response")
        void shouldMaintainTransactionOrderInResponse() {
            // Given - Transactions in specific order
            String accountId = "acc-order";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "FIRST TRANSACTION", "-10.00", "Store A"),
                new BankTransactionData("2024-01-16", "SECOND TRANSACTION", "-20.00", "Store B"),
                new BankTransactionData("2024-01-17", "THIRD TRANSACTION", "-30.00", "Store C")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            assertThat(response.successfulImports()).isEqualTo(3);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            assertThat(importedTransactions).hasSize(3);
            assertThat(importedTransactions.get(0).description()).isEqualTo("FIRST TRANSACTION");
            assertThat(importedTransactions.get(1).description()).isEqualTo("SECOND TRANSACTION");
            assertThat(importedTransactions.get(2).description()).isEqualTo("THIRD TRANSACTION");
        }

        @Test
        @DisplayName("Should generate unique transaction IDs")
        void shouldGenerateUniqueTransactionIds() {
            // Given
            String accountId = "acc-unique-ids";
            List<BankTransactionData> transactions = List.of(
                new BankTransactionData("2024-01-15", "TRANSACTION 1", "-10.00", "Store"),
                new BankTransactionData("2024-01-16", "TRANSACTION 2", "-20.00", "Store"),
                new BankTransactionData("2024-01-17", "TRANSACTION 3", "-30.00", "Store")
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(accountId, transactions);

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            
            List<BankTransaction> importedTransactions = response.importedTransactions();
            Set<String> transactionIds = importedTransactions.stream()
                .map(transaction -> transaction.id().value())
                .collect(java.util.stream.Collectors.toSet());
                
            // All IDs should be unique
            assertThat(transactionIds).hasSize(3);
        }

        @Test
        @DisplayName("Should set transaction fields correctly")
        void shouldSetTransactionFieldsCorrectly() {
            // Given
            String accountId = "acc-fields";
            BankTransactionData transactionData = new BankTransactionData(
                "2024-01-15",
                "TEST TRANSACTION",
                "-50.00",
                "Test Store"
            );
            ImportBankTransactionsRequest request = new ImportBankTransactionsRequest(
                accountId, 
                List.of(transactionData)
            );

            // When
            ImportBankTransactionsResponse response = importBankTransactions.importTransactions(request);

            // Then
            assertThat(response.result()).isEqualTo(ImportResult.SUCCESS);
            
            BankTransaction importedTransaction = response.importedTransactions().get(0);
            
            // Verify all required fields are set
            assertThat(importedTransaction.id()).isNotNull();
            assertThat(importedTransaction.accountId()).isEqualTo(AccountId.of(accountId));
            assertThat(importedTransaction.date()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(importedTransaction.description()).isEqualTo("TEST TRANSACTION");
            assertThat(importedTransaction.merchantName()).isEqualTo("Test Store");
            assertThat(importedTransaction.amount()).isEqualTo(Money.of(new BigDecimal("-50.00")));
            assertThat(importedTransaction.transactionType()).isEqualTo("DEBIT");
            
            // Verify optional fields are set to expected defaults
            assertThat(importedTransaction.memo()).isNull();
            assertThat(importedTransaction.reference()).isNull();
            assertThat(importedTransaction.inferredCategory().name()).isEqualTo("Uncategorized");
        }
    }
}