package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Web request DTO for reconciling transactions between YNAB and bank account.
 * Uses string representations for HTTP transport optimization.
 * Account ID is provided via URL path parameter.
 */
public record ReconcileTransactionsWebRequest(
        @NotNull(message = "From date is required")
        LocalDate fromDate,
        
        @NotNull(message = "To date is required") 
        LocalDate toDate,
        
        @NotBlank(message = "Reconciliation strategy is required")
        @Pattern(regexp = "STRICT|FUZZY", message = "Strategy must be STRICT or FUZZY")
        String reconciliationStrategy
) {
}