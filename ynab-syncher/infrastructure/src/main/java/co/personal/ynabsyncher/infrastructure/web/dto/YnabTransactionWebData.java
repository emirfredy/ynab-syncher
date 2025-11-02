package co.personal.ynabsyncher.infrastructure.web.dto;

import java.time.LocalDate;

/**
 * Web DTO representing YNAB transaction data for HTTP transport.
 */
public record YnabTransactionWebData(
        String id,
        String accountId,
        String categoryId,
        String payeeId,
        String memo,
        String amount,
        LocalDate date,
        boolean cleared,
        boolean approved
) {
}