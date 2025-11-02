package co.personal.ynabsyncher.infrastructure.web.dto.mapper;

import co.personal.ynabsyncher.api.dto.BankTransactionData;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.infrastructure.web.dto.BankTransactionWebData;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting sync transactions requests and responses between web and domain layers.
 */
@Component
public class SyncTransactionsWebMapper {

    private final ImportBankTransactionsWebMapper importMapper;
    private final ReconcileTransactionsWebMapper reconcileMapper;

    public SyncTransactionsWebMapper(
            ImportBankTransactionsWebMapper importMapper,
            ReconcileTransactionsWebMapper reconcileMapper) {
        this.importMapper = importMapper;
        this.reconcileMapper = reconcileMapper;
    }

    /**
     * Converts sync web request to import domain request.
     */
    public ImportBankTransactionsRequest toImportRequest(SyncTransactionsWebRequest webRequest) {
        List<BankTransactionData> domainTransactions = webRequest.transactions().stream()
                .map(this::toDomainTransactionData)
                .toList();
        
        return new ImportBankTransactionsRequest(
                webRequest.accountId(),
                domainTransactions
        );
    }

    /**
     * Converts all operation results to sync web response.
     */
    public SyncTransactionsWebResponse toWebResponse(
            ImportBankTransactionsResponse importResponse,
            ReconciliationResult reconciliationResult,
            CreateMissingTransactionsResponse createResponse) {
        
        ImportBankTransactionsWebResponse importWebResponse = importMapper.toWebResponse(importResponse);
        ReconcileTransactionsWebResponse reconcileWebResponse = reconcileMapper.toWebResponse(reconciliationResult);
        CreateMissingTransactionsWebResponse createWebResponse = convertCreateResponse(createResponse);
        
        boolean syncCompleted = importResponse.isSuccessful() && 
                               reconciliationResult.isFullyReconciled() &&
                               (createResponse == null || createResponse.failed() == 0);
        
        String status = syncCompleted ? "SUCCESS" : "PARTIAL_SUCCESS";
        String message = generateSyncMessage(importResponse, reconciliationResult, createResponse);
        
        return new SyncTransactionsWebResponse(
                importWebResponse,
                reconcileWebResponse,
                createWebResponse,
                syncCompleted,
                status,
                message
        );
    }

    private CreateMissingTransactionsWebResponse convertCreateResponse(CreateMissingTransactionsResponse createResponse) {
        if (createResponse == null) {
            return null;
        }
        
        List<String> createdIds = createResponse.results().stream()
                .filter(result -> result.wasSuccessful())
                .map(result -> result.ynabTransactionId().map(Object::toString).orElse(""))
                .filter(id -> !id.isEmpty())
                .toList();
        
        List<String> errors = createResponse.results().stream()
                .filter(result -> !result.wasSuccessful())
                .map(result -> result.errorMessage().orElse("Unknown error"))
                .toList();
        
        return new CreateMissingTransactionsWebResponse(
                createResponse.totalProcessed(),
                createResponse.successfullyCreated(),
                createResponse.failed(),
                createdIds,
                errors
        );
    }

    private String generateSyncMessage(
            ImportBankTransactionsResponse importResponse,
            ReconciliationResult reconciliationResult,
            CreateMissingTransactionsResponse createResponse) {
        
        StringBuilder message = new StringBuilder();
        message.append("Sync completed: ");
        message.append(importResponse.successfulImports()).append(" transactions imported, ");
        message.append(reconciliationResult.getMatchedCount()).append(" matched, ");
        message.append(reconciliationResult.missingFromYnab().size()).append(" missing from YNAB");
        
        if (createResponse != null) {
            message.append(", ").append(createResponse.successfullyCreated()).append(" created in YNAB");
        }
        
        return message.toString();
    }

    private BankTransactionData toDomainTransactionData(BankTransactionWebData webData) {
        return new BankTransactionData(
                webData.date(),
                webData.description(),
                webData.amount(),
                webData.merchantName()
        );
    }
}