package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Web DTO for creating missing transactions requests.
 * 
 * Optimized for HTTP transport with JSON-friendly types and Bean Validation.
 * Used by REST controllers to receive transaction creation requests from clients.
 */
public record CreateMissingTransactionsWebRequest(
        
        @NotBlank(message = "Budget ID cannot be blank")
        String budgetId,
        
        @NotBlank(message = "YNAB account ID cannot be blank")
        String ynabAccountId,
        
        @NotNull(message = "Missing transactions list cannot be null")
        @NotEmpty(message = "Missing transactions list cannot be empty")
        @Valid
        List<BankTransactionWebData> missingTransactions
) {
}