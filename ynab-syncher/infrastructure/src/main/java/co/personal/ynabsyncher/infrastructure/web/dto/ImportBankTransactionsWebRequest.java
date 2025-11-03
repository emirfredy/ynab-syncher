package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Web request DTO for importing bank transactions from external data sources.
 * Optimized for HTTP transport with string-based validation.
 * Account ID is provided via URL path parameter.
 */
public record ImportBankTransactionsWebRequest(
        @NotNull(message = "Transactions list is required")
        @NotEmpty(message = "Transactions list cannot be empty")
        @Valid
        List<BankTransactionWebData> transactions
) {
}