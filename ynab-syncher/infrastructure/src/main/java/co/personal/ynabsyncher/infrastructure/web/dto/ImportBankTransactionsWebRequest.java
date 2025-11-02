package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Web request DTO for importing bank transactions from external data sources.
 * Optimized for HTTP transport with string-based validation.
 */
public record ImportBankTransactionsWebRequest(
        @NotBlank(message = "Account ID is required")
        String accountId,
        
        @NotNull(message = "Transactions list is required")
        @NotEmpty(message = "Transactions list cannot be empty")
        @Valid
        List<BankTransactionWebData> transactions
) {
}