package co.personal.ynabsyncher.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Web DTO representing raw bank transaction data for HTTP transport.
 * Optimized for JSON serialization with string-based validation.
 */
public record BankTransactionWebData(
        @NotBlank(message = "Date is required")
        String date,
        
        @NotBlank(message = "Description is required") 
        String description,
        
        @NotBlank(message = "Amount is required")
        String amount,
        
        String merchantName  // Optional field
) {
}