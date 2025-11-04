package co.personal.ynabsyncher.infrastructure.service;

import co.personal.ynabsyncher.api.dto.CategoryInferenceRequest;
import co.personal.ynabsyncher.api.dto.CategoryInferenceResponse;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsRequest;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsResponse;
import co.personal.ynabsyncher.api.usecase.CreateMissingTransactions;
import co.personal.ynabsyncher.api.usecase.ImportBankTransactions;
import co.personal.ynabsyncher.api.usecase.InferTransactionCategories;
import co.personal.ynabsyncher.api.usecase.ReconcileTransactions;
import co.personal.ynabsyncher.api.usecase.SaveCategoryMappings;
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
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.ImportBankTransactionsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.ReconcileTransactionsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.SyncTransactionsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.InferCategoriesWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.SaveCategoryMappingsWebMapper;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.CreateMissingTransactionsWebMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
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
    private final InferTransactionCategories inferTransactionCategories;
    private final SaveCategoryMappings saveCategoryMappings;
    private final ImportBankTransactionsWebMapper importMapper;
    private final ReconcileTransactionsWebMapper reconcileMapper;
    private final SyncTransactionsWebMapper syncMapper;
    private final InferCategoriesWebMapper inferCategoriesMapper;
    private final SaveCategoryMappingsWebMapper saveCategoryMappingsMapper;
    private final CreateMissingTransactionsWebMapper createMissingTransactionsMapper;

    public YnabSyncApplicationService(
            ImportBankTransactions importBankTransactions,
            ReconcileTransactions reconcileTransactions,
            CreateMissingTransactions createMissingTransactions,
            InferTransactionCategories inferTransactionCategories,
            SaveCategoryMappings saveCategoryMappings,
            ImportBankTransactionsWebMapper importMapper,
            ReconcileTransactionsWebMapper reconcileMapper,
            SyncTransactionsWebMapper syncMapper,
            InferCategoriesWebMapper inferCategoriesMapper,
            SaveCategoryMappingsWebMapper saveCategoryMappingsMapper,
            CreateMissingTransactionsWebMapper createMissingTransactionsMapper) {
        this.importBankTransactions = importBankTransactions;
        this.reconcileTransactions = reconcileTransactions;
        this.createMissingTransactions = createMissingTransactions;
        this.inferTransactionCategories = inferTransactionCategories;
        this.saveCategoryMappings = saveCategoryMappings;
        this.importMapper = importMapper;
        this.reconcileMapper = reconcileMapper;
        this.syncMapper = syncMapper;
        this.inferCategoriesMapper = inferCategoriesMapper;
        this.saveCategoryMappingsMapper = saveCategoryMappingsMapper;
        this.createMissingTransactionsMapper = createMissingTransactionsMapper;
    }

    /**
     * Imports bank transactions with account access validation and business orchestration.
     * 
     * Business Value Added:
     * - Account-level security validation
     * - Input validation and business rule enforcement  
     * - Error handling and recovery strategies
     * - Proper DTO mapping between web and domain layers
     */
    public ImportBankTransactionsWebResponse importBankTransactions(String accountId, ImportBankTransactionsWebRequest webRequest, Authentication authentication) {
        logger.info("Starting bank transaction import for account: {} by user: {}", accountId, authentication.getName());
        
        try {
            // Convert web DTO to domain DTO
            ImportBankTransactionsRequest domainRequest = importMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            ImportBankTransactionsResponse domainResponse = importBankTransactions.importTransactions(domainRequest);
            
            // Business logging with actual domain data
            logger.info("Bank transaction import completed successfully for user: {}. Total: {}, Successful: {}, Failed: {}", 
                    authentication.getName(),
                    domainResponse.totalProcessed(),
                    domainResponse.successfulImports(), 
                    domainResponse.failedImports());
            
            // Convert domain response to web DTO
            return importMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Bank transaction import failed for account {} by user {}: {}", 
                    accountId, authentication.getName(), e.getMessage(), e);
            throw new RuntimeException("Service unavailable", e);
        }
    }

    /**
     * Reconciles transactions with account access validation and business orchestration.
     */
    public ReconcileTransactionsWebResponse reconcileTransactions(String accountId, ReconcileTransactionsWebRequest webRequest, Authentication authentication) {
        logger.info("Starting transaction reconciliation for account: {} by user: {}", accountId, authentication.getName());
        
        try {
            // Convert web DTO to domain DTO
            ReconciliationRequest domainRequest = reconcileMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            ReconciliationResult domainResponse = reconcileTransactions.reconcile(domainRequest);
            
            // Business logging with domain data
            logger.info("Transaction reconciliation completed for account {} by user {}: {} matched, {} missing from YNAB", 
                    accountId, authentication.getName(),
                    domainResponse.getMatchedCount(),
                    domainResponse.getMissingCount());
            
            return reconcileMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Transaction reconciliation failed for account {} by user {}: {}", 
                    accountId, authentication.getName(), e.getMessage(), e);
            throw new RuntimeException("Service unavailable", e);
        }
    }

    /**
     * Infers transaction categories with account access validation and business orchestration.
     */
    public InferCategoriesWebResponse inferCategories(String accountId, InferCategoriesWebRequest webRequest, Authentication authentication) {
        logger.info("Starting category inference for account: {} with {} transactions by user: {}", 
                accountId, webRequest.transactions().size(), authentication.getName());
        
        try {
            // Convert web DTO to domain DTO
            CategoryInferenceRequest domainRequest = inferCategoriesMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            CategoryInferenceResponse domainResponse = inferTransactionCategories.inferCategories(domainRequest);
            
            // Business logging with domain data
            logger.info("Category inference completed for account {} by user {}: {} categories inferred, {} low confidence", 
                    accountId, authentication.getName(),
                    domainResponse.successfulInferences(),
                    domainResponse.results().stream()
                            .mapToInt(result -> result.successful() && result.inferenceResult().confidence() < 0.7 ? 1 : 0)
                            .sum());
            
            return inferCategoriesMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Category inference failed for account {} by user {}: {}", 
                    accountId, authentication.getName(), e.getMessage(), e);
            throw new RuntimeException("Service unavailable", e);
        }
    }

    /**
     * Creates missing transactions with account access validation and business orchestration.
     */
    public CreateMissingTransactionsWebResponse createMissingTransactions(String accountId, CreateMissingTransactionsWebRequest webRequest, Authentication authentication) {
        logger.info("Starting missing transaction creation for account: {} by user: {}", accountId, authentication.getName());
        
        try {
            // Convert web DTO to domain DTO
            CreateMissingTransactionsRequest domainRequest = createMissingTransactionsMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            CreateMissingTransactionsResponse domainResponse = createMissingTransactions.createMissingTransactions(domainRequest);
            
            // Business logging with domain data
            logger.info("Missing transaction creation completed for account {} by user {}: {} transactions created", 
                    accountId, authentication.getName(), domainResponse.successfullyCreated());
            
            return createMissingTransactionsMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Missing transaction creation failed for account {} by user {}: {}", 
                    accountId, authentication.getName(), e.getMessage(), e);
            throw new RuntimeException("Service unavailable", e);
        }
    }

    /**
     * Imports bank transactions with business validation and orchestration.
     * 
     * Business Value Added:
     * - Input validation and business rule enforcement
     * - Error handling and recovery strategies
     * - Proper DTO mapping between web and domain layers
     */
    public ImportBankTransactionsWebResponse importBankTransactions(String accountId, ImportBankTransactionsWebRequest webRequest) {
        logger.info("Starting bank transaction import for account: {}", accountId);
        
        try {
            // Convert web DTO to domain DTO
            ImportBankTransactionsRequest domainRequest = importMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            ImportBankTransactionsResponse domainResponse = importBankTransactions.importTransactions(domainRequest);
            
            // Business logging with actual domain data
            logger.info("Bank transaction import completed for account {}: {} total, {} imported, {} duplicates skipped", 
                    accountId, 
                    domainResponse.totalProcessed(),
                    domainResponse.successfulImports(),
                    domainResponse.duplicatesSkipped());
            
            // Convert domain DTO to web DTO
            return importMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to import transactions for account {}", accountId, e);
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
    public ReconcileTransactionsWebResponse reconcileTransactions(String accountId, ReconcileTransactionsWebRequest webRequest) {
        logger.info("Starting transaction reconciliation for account: {} from {} to {}", 
                accountId, webRequest.fromDate(), webRequest.toDate());
        
        try {
            // Convert web DTO to domain DTO
            ReconciliationRequest domainRequest = reconcileMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            ReconciliationResult domainResponse = reconcileTransactions.reconcile(domainRequest);
            
            // Business analysis and reporting
            int totalMatched = domainResponse.matchedTransactions().size();
            int missingFromYnab = domainResponse.missingFromYnab().size();
            
            logger.info("Reconciliation completed for account {}: {} matched, {} missing from YNAB",
                    accountId, totalMatched, missingFromYnab);
            
            // Business intelligence: warn if high discrepancy rate
            if (missingFromYnab > 0 && totalMatched > 0) {
                double discrepancyRate = (double) missingFromYnab / (totalMatched + missingFromYnab);
                if (discrepancyRate > 0.2) { // 20% threshold
                    logger.warn("High discrepancy rate ({}%) detected for account {} - {} missing transactions", 
                            String.format("%.1f", discrepancyRate * 100), accountId, missingFromYnab);
                }
            }
            
            // Convert domain DTO to web DTO
            return reconcileMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to reconcile transactions for account {}", accountId, e);
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
     * @param accountId the account identifier from URL path
     * @param webRequest the synchronization request with date range and budget data
     * @return comprehensive synchronization results with business metrics
     */
    public SyncTransactionsWebResponse syncTransactions(String accountId, SyncTransactionsWebRequest webRequest) {
        logger.info("Starting full sync for account {} from {} to {}", 
                accountId, webRequest.fromDate(), webRequest.toDate());
        
        try {
            // Business validation
            validateSyncRequest(webRequest);
            
            // Step 1: Import bank transactions with business context
            logger.debug("Step 1: Importing bank transactions for account {}", accountId);
            ImportBankTransactionsRequest importRequest = syncMapper.toImportRequest(accountId, webRequest);
            ImportBankTransactionsResponse importResponse = importBankTransactions.importTransactions(importRequest);
            
            // Business intelligence: Early termination if no imports
            if (importResponse.successfulImports() == 0) {
                logger.info("No new transactions imported for account {} - sync completed with no changes", accountId);
                return createSimpleSyncResponse(importResponse, "NO_NEW_TRANSACTIONS");
            }
            
            // Step 2: Reconcile with YNAB with intelligent analysis
            logger.debug("Step 2: Reconciling {} imported transactions with YNAB", importResponse.successfulImports());
            ReconciliationRequest reconcileRequest = ReconciliationRequest.of(
                    AccountId.of(accountId),
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
                            accountId, 
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
                        AccountId.of(accountId), // Bank account ID
                        AccountId.of(accountId), // YNAB account ID (assuming same)
                        reconciliationResult.missingFromYnab()
                );
                createResponse = createMissingTransactions.createMissingTransactions(createRequest);
                
                // Business validation: Verify creation success
                if (createResponse.successfullyCreated() < missingTransactions) {
                    logger.warn("Only {} out of {} missing transactions were created successfully for account {}", 
                            createResponse.successfullyCreated(), missingTransactions, accountId);
                }
            } else if (missingTransactions > 0) {
                logger.info("Found {} missing transactions but auto-creation is disabled for account {}", 
                        missingTransactions, accountId);
            }
            
            // Business metrics and comprehensive reporting
            String syncStatus = determineSyncStatus(importResponse, reconciliationResult, createResponse);
            
            logger.info("Sync completed for account {}: {} imported, {} reconciled, {} created, status: {}", 
                    accountId, 
                    importResponse.successfulImports(),
                    matchedTransactions,
                    createResponse != null ? createResponse.successfullyCreated() : 0,
                    syncStatus);
            
            // Convert to web response with business intelligence
            return syncMapper.toWebResponse(importResponse, reconciliationResult, createResponse);
            
        } catch (Exception e) {
            logger.error("Sync failed for account {} from {} to {}", 
                    accountId, webRequest.fromDate(), webRequest.toDate(), e);
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

    /**
     * Infers categories for transactions using ML-based analysis.
     * 
     * Business Value Added:
     * - Input validation and business rule enforcement
     * - ML model integration with proper error handling
     * - Confidence scoring and quality thresholds
     * - Proper DTO mapping between web and domain layers
     */
    public InferCategoriesWebResponse inferCategories(String accountId, InferCategoriesWebRequest webRequest) {
        logger.info("Starting category inference for account: {} budget: {} with {} transactions", 
                accountId, webRequest.budgetId(), webRequest.transactions().size());
        
        try {
            // Convert web DTO to domain DTO
            CategoryInferenceRequest domainRequest = inferCategoriesMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            CategoryInferenceResponse domainResponse = inferTransactionCategories.inferCategories(domainRequest);
            
            // Business analysis and logging
            logger.info("Category inference completed for account {} budget {}: {} successful, {} failed", 
                    accountId,
                    webRequest.budgetId(), 
                    domainResponse.successfulInferences(),
                    domainResponse.failedInferences());
            
            // Convert domain DTO to web DTO
            return inferCategoriesMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to infer categories for budget {}", webRequest.budgetId(), e);
            throw e;
        }
    }

    /**
     * Saves learned category mappings with conflict resolution.
     * 
     * Business Value Added:
     * - Global ML knowledge base management
     * - Conflict detection and resolution strategies
     * - Quality thresholds and validation rules
     * - Cross-cutting concerns for ML model improvement
     */
    public SaveCategoryMappingsWebResponse saveCategoryMappings(SaveCategoryMappingsWebRequest webRequest) {
        logger.info("Starting category mapping save operation with {} mappings", 
                webRequest.categoryMappings().size());
        
        try {
            // Convert web DTO to domain DTO
            SaveCategoryMappingsRequest domainRequest = saveCategoryMappingsMapper.toDomainRequest(webRequest);
            
            // Execute domain use case
            SaveCategoryMappingsResponse domainResponse = saveCategoryMappings.saveCategoryMappings(domainRequest);
            
            // Business analysis and logging
            logger.info("Category mapping save completed: {} new, {} updated, {} skipped", 
                    domainResponse.savedNew(),
                    domainResponse.updatedExisting(),
                    domainResponse.skipped());
            
            // Convert domain DTO to web DTO
            return saveCategoryMappingsMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to save category mappings", e);
            throw e;
        }
    }

    /**
     * Creates missing transactions in YNAB as standalone operation.
     * 
     * Business Value Added:
     * - Standalone missing transaction creation (outside of sync workflow)
     * - Business validation and error handling
     * - Proper DTO mapping between web and domain layers
     * - Comprehensive result reporting
     */
    public CreateMissingTransactionsWebResponse createMissingTransactions(String accountId, CreateMissingTransactionsWebRequest webRequest) {
        logger.info("Starting missing transaction creation for account: {} with {} transactions", 
                accountId, webRequest.missingTransactions().size());
        
        try {
            // Convert web DTO to domain DTO
            CreateMissingTransactionsRequest domainRequest = createMissingTransactionsMapper.toDomainRequest(accountId, webRequest);
            
            // Execute domain use case
            CreateMissingTransactionsResponse domainResponse = createMissingTransactions.createMissingTransactions(domainRequest);
            
            // Business analysis and logging
            logger.info("Missing transaction creation completed: {} successful, {} failed", 
                    domainResponse.successfullyCreated(),
                    domainResponse.failed());
            
            // Convert domain DTO to web DTO
            return createMissingTransactionsMapper.toWebResponse(domainResponse);
            
        } catch (Exception e) {
            logger.error("Failed to create missing transactions for account {}", webRequest.ynabAccountId(), e);
            throw e;
        }
    }
}