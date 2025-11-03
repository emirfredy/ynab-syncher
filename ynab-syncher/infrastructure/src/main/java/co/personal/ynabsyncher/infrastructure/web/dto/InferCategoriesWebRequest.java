package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Web DTO for category inference requests.
 * 
 * Optimized for HTTP transport with JSON-friendly types and Bean Validation.
 * Used by REST controllers to receive category inference requests from clients.
 */
public record InferCategoriesWebRequest(
        
        @NotBlank(message = "Budget ID cannot be blank")
        String budgetId,
        
        @NotNull(message = "Transactions list cannot be null")
        @NotEmpty(message = "Transactions list cannot be empty")
        @Valid
        List<BankTransactionWebData> transactions
) {
}