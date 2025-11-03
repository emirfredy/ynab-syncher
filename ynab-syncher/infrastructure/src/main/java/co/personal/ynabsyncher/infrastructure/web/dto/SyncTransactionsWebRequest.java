package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * Web request DTO for complete YNAB synchronization workflow.
 * Combines import, reconciliation, and creation parameters.
 * Account ID is provided via URL path parameter.
 */
public record SyncTransactionsWebRequest(
        @NotBlank(message = "Budget ID is required")
        String budgetId,
        
        @NotNull(message = "From date is required")
        LocalDate fromDate,
        
        @NotNull(message = "To date is required")
        LocalDate toDate,
        
        @NotBlank(message = "Reconciliation strategy is required")
        @Pattern(regexp = "STRICT|FUZZY", message = "Strategy must be STRICT or FUZZY")
        String reconciliationStrategy,
        
        @NotNull(message = "Transactions list is required")
        @NotEmpty(message = "Transactions list cannot be empty")
        @Valid
        List<BankTransactionWebData> transactions,
        
        boolean createMissingTransactions
) {
}