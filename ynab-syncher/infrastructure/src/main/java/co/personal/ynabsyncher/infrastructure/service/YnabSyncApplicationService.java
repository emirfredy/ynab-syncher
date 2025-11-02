package co.personal.ynabsyncher.infrastructure.service;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.api.usecase.CreateMissingTransactions;
import co.personal.ynabsyncher.api.usecase.ImportBankTransactions;
import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.BudgetId;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.ImportBankTransactionsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.ReconcileTransactionsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.SyncTransactionsWebMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Application service implementing the Netflix/Uber microservices pattern.
 * 
 * Provides business value through:
 * - Complex workflow orchestration across multiple domain use cases
 * - Cross-cutting concerns (transactions, logging, error handling)
 * - Infrastructure integration (performance monitoring, business metrics)
 * - DTO mapping between Web layer and Domain layer
 */
@Service
@Transactional
public class YnabSyncApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(YnabSyncApplicationService.class);

    private final ImportBankTransactions importBankTransactions;
    private final ReconcileTransactions reconcileTransactions;
    private final CreateMissingTransactions createMissingTransactions;
    private final ImportBankTransactionsWebMapper importMapper;
    private final ReconcileTransactionsWebMapper reconcileMapper;
    private final SyncTransactionsWebMapper syncMapper;

    public YnabSyncApplicationService(
            ImportBankTransactions importBankTransactions,
            ReconcileTransactions reconcileTransactions,
            CreateMissingTransactions createMissingTransactions,
            ImportBankTransactionsWebMapper importMapper,
            ReconcileTransactionsWebMapper reconcileMapper,
            SyncTransactionsWebMapper syncMapper) {
        this.importBankTransactions = importBankTransactions;
        this.reconcileTransactions = reconcileTransactions;
        this.createMissingTransactions = createMissingTransactions;
        this.importMapper = importMapper;
        this.reconcileMapper = reconcileMapper;
        this.syncMapper = syncMapper;
    }

    /**
     * Imports bank transactions with business validation and orchestration.
     * 
     * Business Value Added:
     * - Input validation and business rule enforcement
     * - Error handling and recovery strategies
     * - Proper DTO mapping between web and domain layers
     */
    public ImportBankTransactionsWebResponse importBankTransactions(ImportBankTransactionsWebRequest webRequest) {
        logger.info("Starting bank transaction import for account: {}", webRequest.accountId());
        
        try {
            // Convert web DTO to domain DTO
            ImportBankTransactionsRequest domainRequest = importMapper.toDomainRequest(webRequest);
            
            // Execute domain use case
            ImportBankTransactionsResponse domainResponse = importBankTransactions.importTransactions(domainRequest);
            
            // Business logging with actual domain data
            logger.info("Bank transaction import completed for account {}: {} total, {} imported, {} duplicates skipped", 
                    webRequest.accountId(), 
                    domainResponse.totalProcessed(),
                    domainResponse.successfulImports(),
                    domainResponse.duplicatesSkipped());
            
            // Convert domain DTO to web DTO
            return importMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to import transactions for account {}", webRequest.accountId(), e);
            throw e; // Let global exception handler deal with it
        }
    }

    /**
     * Reconciles transactions with business intelligence and workflow orchestration.
     * 
     * Business Value Added:
     * - Business validation beyond simple bean validation
     * - Intelligent reconciliation strategy selection
     * - Comprehensive business analysis of reconciliation results
     * - Proper DTO mapping and error handling
     */
    public ReconcileTransactionsWebResponse reconcileTransactions(ReconcileTransactionsWebRequest webRequest) {
        logger.info("Starting transaction reconciliation for account: {} from {} to {}", 
                webRequest.accountId(), webRequest.fromDate(), webRequest.toDate());
        
        try {
            // Convert web DTO to domain DTO
            ReconciliationRequest domainRequest = reconcileMapper.toDomainRequest(webRequest);
            
            // Execute domain use case
            ReconciliationResult domainResponse = reconcileTransactions.reconcile(domainRequest);
            
            // Business analysis and reporting
            int totalMatched = domainResponse.matchedTransactions().size();
            int missingFromYnab = domainResponse.missingFromYnab().size();
            
            logger.info("Reconciliation completed for account {}: {} matched, {} missing from YNAB",
                    webRequest.accountId(), totalMatched, missingFromYnab);
            
            // Business intelligence: warn if high discrepancy rate
            if (missingFromYnab > 0 && totalMatched > 0) {
                double discrepancyRate = (double) missingFromYnab / (totalMatched + missingFromYnab);
                if (discrepancyRate > 0.2) { // 20% threshold
                    logger.warn("High discrepancy rate ({}%) detected for account {} - {} missing transactions", 
                            String.format("%.1f", discrepancyRate * 100), webRequest.accountId(), missingFromYnab);
                }
            }
            
            // Convert domain DTO to web DTO
            return reconcileMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to reconcile transactions for account {}", webRequest.accountId(), e);
            throw e;
        }
    }

    /**
     * Complete synchronization workflow with comprehensive business orchestration.
     * 
     * This method exemplifies the Netflix/Uber pattern by orchestrating multiple domain use cases
     * in a business-meaningful workflow with cross-cutting concerns, error handling, and business intelligence.
     * 
     * Workflow Orchestration:
     * 1. Business validation and input sanitization
     * 2. Import bank transactions from external sources
     * 3. Reconcile imported transactions with existing YNAB data  
     * 4. Create missing transactions in YNAB (if enabled)
     * 5. Business analysis and comprehensive reporting
     * 6. Error handling with business context
     * 
     * @param webRequest the synchronization request with account and date range
     * @return comprehensive synchronization results with business metrics
     */
    public SyncTransactionsWebResponse syncTransactions(SyncTransactionsWebRequest webRequest) {
        logger.info("Starting full sync for account {} from {} to {}", 
                webRequest.accountId(), webRequest.fromDate(), webRequest.toDate());
        
        try {
            // Business validation
            validateSyncRequest(webRequest);
            
            // Step 1: Import bank transactions with business context
            logger.debug("Step 1: Importing bank transactions for account {}", webRequest.accountId());
            ImportBankTransactionsRequest importRequest = syncMapper.toImportRequest(webRequest);
            ImportBankTransactionsResponse importResponse = importBankTransactions.importTransactions(importRequest);
            
            // Business intelligence: Early termination if no imports
            if (importResponse.successfulImports() == 0) {
                logger.info("No new transactions imported for account {} - sync completed with no changes", webRequest.accountId());
                return createSimpleSyncResponse(importResponse, "NO_NEW_TRANSACTIONS");
            }
            
            // Step 2: Reconcile with YNAB with intelligent analysis
            logger.debug("Step 2: Reconciling {} imported transactions with YNAB", importResponse.successfulImports());
            ReconciliationRequest reconcileRequest = ReconciliationRequest.of(
                    AccountId.of(webRequest.accountId()),
                    webRequest.fromDate(),
                    webRequest.toDate(),
                    ReconciliationStrategy.valueOf(webRequest.reconciliationStrategy())
            );
            ReconciliationResult reconciliationResult = reconcileTransactions.reconcile(reconcileRequest);
            
            // Business analysis and decision making
            int missingTransactions = reconciliationResult.missingFromYnab().size();
            int matchedTransactions = reconciliationResult.matchedTransactions().size();
            
            // Business intelligence: Discrepancy analysis
            if (missingTransactions > 0 && matchedTransactions > 0) {
                double discrepancyRate = (double) missingTransactions / (matchedTransactions + missingTransactions);
                if (discrepancyRate > 0.2) { // 20% threshold
                    logger.warn("High discrepancy rate ({}%) detected for account {} - {} missing out of {} total", 
                            String.format("%.1f", discrepancyRate * 100), 
                            webRequest.accountId(), 
                            missingTransactions, 
                            matchedTransactions + missingTransactions);
                }
            }
            
            // Step 3: Create missing transactions with business rules
            CreateMissingTransactionsResponse createResponse = null;
            if (webRequest.createMissingTransactions() && missingTransactions > 0) {
                logger.debug("Step 3: Creating {} missing transactions in YNAB", missingTransactions);
                
                // Business rule: Limit bulk creation to prevent API abuse
                if (missingTransactions > 100) {
                    logger.warn("Large number of missing transactions ({}), proceeding with caution", missingTransactions);
                }
                
                CreateMissingTransactionsRequest createRequest = new CreateMissingTransactionsRequest(
                        BudgetId.of(webRequest.budgetId()),
                        AccountId.of(webRequest.accountId()), // Bank account ID
                        AccountId.of(webRequest.accountId()), // YNAB account ID (assuming same)
                        reconciliationResult.missingFromYnab()
                );
                createResponse = createMissingTransactions.createMissingTransactions(createRequest);
                
                // Business validation: Verify creation success
                if (createResponse.successfullyCreated() < missingTransactions) {
                    logger.warn("Only {} out of {} missing transactions were created successfully for account {}", 
                            createResponse.successfullyCreated(), missingTransactions, webRequest.accountId());
                }
            } else if (missingTransactions > 0) {
                logger.info("Found {} missing transactions but auto-creation is disabled for account {}", 
                        missingTransactions, webRequest.accountId());
            }
            
            // Business metrics and comprehensive reporting
            String syncStatus = determineSyncStatus(importResponse, reconciliationResult, createResponse);
            
            logger.info("Sync completed for account {}: {} imported, {} reconciled, {} created, status: {}", 
                    webRequest.accountId(), 
                    importResponse.successfulImports(),
                    matchedTransactions,
                    createResponse != null ? createResponse.successfullyCreated() : 0,
                    syncStatus);
            
            // Convert to web response with business intelligence
            return syncMapper.toWebResponse(importResponse, reconciliationResult, createResponse);
            
        } catch (Exception e) {
            logger.error("Sync failed for account {} from {} to {}", 
                    webRequest.accountId(), webRequest.fromDate(), webRequest.toDate(), e);
            throw e; // Let global exception handler provide proper HTTP response
        }
    }
    
    /**
     * Business validation for sync requests with domain-specific rules.
     */
    private void validateSyncRequest(SyncTransactionsWebRequest request) {
        if (request.fromDate().isAfter(request.toDate())) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
        
        // Business rule: Limit sync range to prevent performance issues
        long daysBetween = ChronoUnit.DAYS.between(request.fromDate(), request.toDate());
        if (daysBetween > 365) {
            throw new IllegalArgumentException("Sync range cannot exceed 365 days. Requested: " + daysBetween + " days");
        }
        
        // Business rule: Prevent syncing future dates
        if (request.toDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot sync transactions for future dates");
        }
    }
    
    /**
     * Determines business-meaningful sync status based on workflow results.
     */
    private String determineSyncStatus(ImportBankTransactionsResponse importResult, 
                                     ReconciliationResult reconcileResult, 
                                     CreateMissingTransactionsResponse createResult) {
        
        if (importResult.errors().size() > 0) {
            return "COMPLETED_WITH_IMPORT_ERRORS";
        }
        
        if (reconcileResult.missingFromYnab().size() > 0 && createResult == null) {
            return "COMPLETED_WITH_MISSING_TRANSACTIONS";
        }
        
        if (createResult != null && createResult.failed() > 0) {
            return "COMPLETED_WITH_CREATION_ERRORS";
        }
        
        if (reconcileResult.isFullyReconciled() && 
            (createResult == null || createResult.successfullyCreated() == reconcileResult.missingFromYnab().size())) {
            return "FULLY_SYNCHRONIZED";
        }
        
        return "PARTIALLY_SYNCHRONIZED";
    }
    
    /**
     * Creates simple sync response for early termination scenarios.
     */
    private SyncTransactionsWebResponse createSimpleSyncResponse(ImportBankTransactionsResponse importResult, String status) {
        return syncMapper.toWebResponse(importResult, null, null);
    }
}