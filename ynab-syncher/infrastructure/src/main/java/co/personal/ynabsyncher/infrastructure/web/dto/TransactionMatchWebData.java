package co.personal.ynabsyncher.infrastructure.web.dto;

/**
 * Web DTO representing a matched transaction pair for HTTP transport.
 */
public record TransactionMatchWebData(
        BankTransactionWebData bankTransaction,
        YnabTransactionWebData ynabTransaction,
        String matchingStrategy,
        double confidenceScore
) {
}