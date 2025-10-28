package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.BankTransactionData;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.api.usecase.ImportBankTransactions;
import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Use case implementation for importing bank transactions from external data sources.
 * Handles validation, duplicate detection, and returns transactions for other use cases.
 * 
 * Framework-free domain implementation following hexagonal architecture.
 */
public class ImportBankTransactionsUseCase implements ImportBankTransactions {

    private static final int MAX_MERCHANT_NAME_LENGTH = 50;

    public ImportBankTransactionsUseCase() {
        // No repository dependency - pure in-memory processing
    }

    @Override
    public ImportBankTransactionsResponse importTransactions(ImportBankTransactionsRequest request) {
        Objects.requireNonNull(request, "Import request cannot be null");

        List<String> errors = new ArrayList<>();
        List<BankTransaction> validTransactions = new ArrayList<>();
        Set<String> processedTransactionHashes = new HashSet<>();
        int duplicatesSkipped = 0;
        int lineNumber = 1;

        AccountId accountId = AccountId.of(request.accountId());

        for (BankTransactionData data : request.transactions()) {
            try {
                // Validate and convert transaction data
                BankTransaction transaction = convertToBankTransaction(accountId, data);
                
                // Check for duplicates within the current import batch
                String transactionHash = createTransactionHash(transaction);
                if (processedTransactionHashes.contains(transactionHash)) {
                    duplicatesSkipped++;
                } else {
                    processedTransactionHashes.add(transactionHash);
                    validTransactions.add(transaction);
                }
                
            } catch (Exception e) {
                errors.add(String.format("Line %d: %s", lineNumber, e.getMessage()));
            }
            lineNumber++;
        }

        // Return response with imported transactions for other use cases
        return createResponse(
            request.transactions().size(), 
            validTransactions.size(), 
            duplicatesSkipped, 
            errors,
            validTransactions
        );
    }

    private String createTransactionHash(BankTransaction transaction) {
        // Create a unique hash based on account, date, amount, and description
        return transaction.accountId() + "|" + 
               transaction.date() + "|" + 
               transaction.amount() + "|" + 
               (transaction.description() != null ? transaction.description() : "");
    }

    private BankTransaction convertToBankTransaction(AccountId accountId, BankTransactionData data) {
        // Validate and parse date
        LocalDate date = parseDate(data.date());
        
        // Validate and parse amount
        Money amount = parseAmount(data.amount());
        
        // Validate description
        if (data.description().isBlank()) {
            throw new IllegalArgumentException("Invalid transaction data: Transaction description cannot be blank");
        }
        
        // Determine merchant name (use provided or derive from description)
        String merchantName = determineMerchantName(data);
        
        return BankTransaction.withUnknownCategory(
            TransactionId.generate(),
            accountId,
            date,
            amount,
            data.description(),
            merchantName,
            null, // memo - not provided in CSV import
            determineTransactionType(amount), // derive from amount sign
            null  // reference - not provided in CSV import
        );
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString);
        }
    }

    private Money parseAmount(String amountString) {
        try {
            BigDecimal amount = new BigDecimal(amountString);
            return Money.of(amount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amountString);
        }
    }

    private String determineMerchantName(BankTransactionData data) {
        String merchantName = data.merchantName();
        
        // Use provided merchant name if available and not blank
        if (merchantName != null && !merchantName.isBlank()) {
            return truncateToMaxLength(merchantName);
        }
        
        // Fallback to description as merchant name
        return truncateToMaxLength(data.description());
    }

    private String truncateToMaxLength(String text) {
        if (text.length() <= MAX_MERCHANT_NAME_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_MERCHANT_NAME_LENGTH);
    }

    private String determineTransactionType(Money amount) {
        return amount.isNegative() ? "DEBIT" : "CREDIT";
    }

    private ImportBankTransactionsResponse createResponse(
        int totalProcessed,
        int successfulImports,
        int duplicatesSkipped,
        List<String> errors,
        List<BankTransaction> importedTransactions
    ) {
        if (errors.isEmpty()) {
            // No errors - all transactions processed successfully (either imported or skipped as duplicates)
            return ImportBankTransactionsResponse.success(successfulImports, duplicatesSkipped, importedTransactions);
        } else if (successfulImports > 0 || duplicatesSkipped > 0) {
            // Mixed results: some success/duplicates but also some errors
            return ImportBankTransactionsResponse.partialSuccess(
                totalProcessed, successfulImports, duplicatesSkipped, errors, importedTransactions
            );
        } else {
            // All failed
            return ImportBankTransactionsResponse.failed(totalProcessed, errors);
        }
    }
}