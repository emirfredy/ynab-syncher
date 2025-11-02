package co.personal.ynabsyncher.infrastructure.web.dto;

/**
 * Web response DTO for complete YNAB synchronization results.
 * Aggregates results from import, reconciliation, and transaction creation steps.
 */
public record SyncTransactionsWebResponse(
        ImportBankTransactionsWebResponse importResults,
        ReconcileTransactionsWebResponse reconciliationResults,
        CreateMissingTransactionsWebResponse creationResults,
        boolean syncCompleted,
        String overallStatus,
        String message
) {
}