package co.personal.ynabsyncher.api.dto;

import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO containing the results of an import bank transactions operation.
 * Returns imported transactions for use by other use cases.
 */
public record ImportBankTransactionsResponse(
    ImportResult result,
    int totalProcessed,
    int successfulImports,
    int duplicatesSkipped,
    List<String> errors,
    List<BankTransaction> importedTransactions
) {
    
    public ImportBankTransactionsResponse {
        Objects.requireNonNull(result, "Import result cannot be null");
        Objects.requireNonNull(errors, "Errors list cannot be null");
        Objects.requireNonNull(importedTransactions, "Imported transactions cannot be null");
    }
    
    /**
     * Calculates the number of failed transactions.
     */
    public int failedImports() {
        return totalProcessed - successfulImports - duplicatesSkipped;
    }
    
    /**
     * Determines if the overall import was successful.
     */
    public boolean isSuccessful() {
        return result == ImportResult.SUCCESS;
    }
    
    /**
     * Calculates the success rate as a percentage (0.0 to 1.0).
     */
    public double successRate() {
        if (totalProcessed == 0) {
            return 0.0;
        }
        return (double) successfulImports / totalProcessed;
    }
    
    /**
     * Creates a successful response.
     */
    public static ImportBankTransactionsResponse success(
            int successfulImports, 
            int duplicatesSkipped,
            List<BankTransaction> importedTransactions) {
        return new ImportBankTransactionsResponse(
            ImportResult.SUCCESS,
            successfulImports + duplicatesSkipped,
            successfulImports,
            duplicatesSkipped,
            List.of(),
            importedTransactions
        );
    }
    
    /**
     * Creates a partial success response.
     */
    public static ImportBankTransactionsResponse partialSuccess(
        int totalProcessed, 
        int successfulImports, 
        int duplicatesSkipped, 
        List<String> errors,
        List<BankTransaction> importedTransactions
    ) {
        return new ImportBankTransactionsResponse(
            ImportResult.PARTIAL_SUCCESS,
            totalProcessed,
            successfulImports,
            duplicatesSkipped,
            errors,
            importedTransactions
        );
    }
    
    /**
     * Creates a failed response.
     */
    public static ImportBankTransactionsResponse failed(int totalProcessed, List<String> errors) {
        return new ImportBankTransactionsResponse(
            ImportResult.FAILED,
            totalProcessed,
            0,
            0,
            errors,
            List.of()  // No successful imports
        );
    }
}