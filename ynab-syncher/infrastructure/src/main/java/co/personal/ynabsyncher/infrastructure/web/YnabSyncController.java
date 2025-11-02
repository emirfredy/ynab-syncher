package co.personal.ynabsyncher.infrastructure.web;

import co.personal.ynabsyncher.infrastructure.service.YnabSyncApplicationService;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for YNAB synchronization operations.
 * 
 * Follows Netflix/Uber pattern by only accessing application services,
 * never directly calling domain use cases. Handles HTTP protocol concerns:
 * - Request/response mapping
 * - Status codes and headers
 * - Correlation ID generation and propagation
 * - Bean validation at boundaries
 */
@RestController
@RequestMapping("/api/v1/ynab-sync")
public class YnabSyncController {

    private static final Logger logger = LoggerFactory.getLogger(YnabSyncController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final YnabSyncApplicationService ynabSyncApplicationService;

    public YnabSyncController(YnabSyncApplicationService ynabSyncApplicationService) {
        this.ynabSyncApplicationService = ynabSyncApplicationService;
    }

    /**
     * Imports bank transactions from external data source.
     * 
     * @param request the import request containing account ID and transaction data
     * @return the import response with results and statistics
     */
    @PostMapping("/import")
    public ResponseEntity<ImportBankTransactionsWebResponse> importBankTransactions(
            @Valid @RequestBody ImportBankTransactionsWebRequest request) {
        
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Starting bank transaction import for account: {}", request.accountId());
            
            ImportBankTransactionsWebResponse response = ynabSyncApplicationService.importBankTransactions(request);
            
            logger.info("Bank transaction import completed successfully. Total: {}, Successful: {}, Failed: {}",
                    response.totalTransactions(), response.successfulImports(), response.failedImports());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to import bank transactions for account: {}", request.accountId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Reconciles transactions between YNAB and bank account for a specific date range.
     * 
     * @param request the reconciliation request containing account, date range, and strategy
     * @return the reconciliation response with matched and missing transactions
     */
    @PostMapping("/reconcile")
    public ResponseEntity<ReconcileTransactionsWebResponse> reconcileTransactions(
            @Valid @RequestBody ReconcileTransactionsWebRequest request) {
        
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Starting transaction reconciliation for account: {} from {} to {}", 
                    request.accountId(), request.fromDate(), request.toDate());
            
            ReconcileTransactionsWebResponse response = ynabSyncApplicationService.reconcileTransactions(request);
            
            logger.info("Transaction reconciliation completed. Matched: {}, Missing from YNAB: {}, Missing from Bank: {}",
                    response.matchedTransactions(), response.missingFromYnab(), response.missingFromBank());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to reconcile transactions for account: {}", request.accountId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Complete synchronization workflow: import bank transactions, reconcile with YNAB,
     * and optionally create missing transactions in YNAB.
     * 
     * @param request the sync request containing all synchronization parameters
     * @return the sync response with complete workflow results
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncTransactionsWebResponse> syncTransactions(
            @Valid @RequestBody SyncTransactionsWebRequest request) {
        
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Starting complete YNAB synchronization for account: {} budget: {} from {} to {}", 
                    request.accountId(), request.budgetId(), request.fromDate(), request.toDate());
            
            SyncTransactionsWebResponse response = ynabSyncApplicationService.syncTransactions(request);
            
            logger.info("YNAB synchronization completed with status: {} - {}", 
                    response.overallStatus(), response.message());
            
            HttpStatus status = response.syncCompleted() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
            
            return ResponseEntity.status(status)
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to sync transactions for account: {} budget: {}", 
                    request.accountId(), request.budgetId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Generates a unique correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}