package co.personal.ynabsyncher.infrastructure.web.dto;

import java.util.List;

/**
 * Web response DTO for bank transaction import results.
 * Contains import statistics and any processing messages.
 */
public record ImportBankTransactionsWebResponse(
        int totalTransactions,
        int successfulImports,
        int failedImports,
        List<String> validationErrors,
        List<String> processingMessages
) {
}