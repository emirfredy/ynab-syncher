package co.personal.ynabsyncher.infrastructure.web.dto;

import java.util.List;

/**
 * Web response DTO for missing transaction creation results.
 */
public record CreateMissingTransactionsWebResponse(
        int totalMissingTransactions,
        int successfulCreations,
        int failedCreations,
        List<String> createdTransactionIds,
        List<String> errors
) {
}