package co.personal.ynabsyncher.infrastructure.web.controller;

import co.personal.ynabsyncher.infrastructure.service.YnabSyncApplicationService;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.InferCategoriesWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SaveCategoryMappingsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.validation.ValidAccountId;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for YNAB-Bank reconciliation operations.
 * 
 * Follows Netflix/Uber pattern by only accessing application services,
 * never directly calling domain use cases. Handles HTTP protocol concerns:
 * - RESTful resource hierarchy: accounts/{accountId}/transactions & category-mappings
 * - Action-oriented business operations aligned with domain vocabulary
 * - Status codes and headers with correlation ID propagation
 * - Bean validation at boundaries
 * 
 * Endpoint Design Rationale:
 * 
 * Account-Scoped Resource Hierarchy: accounts/{accountId}/transactions/* 
 * - Clear parent-child relationship between accounts and transactions
 * - Action-oriented verbs: import, reconcile, infer-categories, create-missing
 * - Business language alignment with domain use cases
 * 
 * Global Resource Hierarchy: category-mappings
 * - Cross-account ML knowledge base management
 * - Global category learning affects all users
 * - Not owned by specific account - global resource scope
 * 
 * POST Semantics: Business operations with side effects, not CRUD operations
 * Request Bodies: Rich request payloads require POST semantics
 * Domain Vocabulary: Terms match use cases and business requirements
 * 
 * Endpoints:
 * - POST /accounts/{accountId}/transactions/import - Import bank transactions
 * - POST /accounts/{accountId}/reconcile - Reconcile bank vs YNAB transactions  
 * - POST /accounts/{accountId}/transactions/infer-categories - ML category inference
 * - POST /accounts/{accountId}/transactions/create-missing - Create missing YNAB transactions
 * - POST /category-mappings - Save learned category mappings (global operation)
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
public class YnabSyncController {

    private static final Logger logger = LoggerFactory.getLogger(YnabSyncController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final YnabSyncApplicationService ynabSyncApplicationService;

    public YnabSyncController(YnabSyncApplicationService ynabSyncApplicationService) {
        this.ynabSyncApplicationService = ynabSyncApplicationService;
    }

    /**
     * Imports bank transactions from external data source for a specific account.
     * 
     * Business Operation: Import bank transaction data into the reconciliation system
     * Workflow Position: Step 1 of YNAB-Bank reconciliation process
     * 
     * Security: Requires USER role or higher (ADMIN also allowed)
     * Phase 3: Account-level authorization - user must own the account
     * 
     * @param accountId the bank account identifier
     * @param request the import request containing transaction data
     * @param authentication the current user's authentication details
     * @return the import response with results and statistics
     */
    @PostMapping("/accounts/{accountId}/transactions/import")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ImportBankTransactionsWebResponse> importBankTransactions(
            @PathVariable @ValidAccountId String accountId,
            @Valid @RequestBody ImportBankTransactionsWebRequest request,
            Authentication authentication) {
        
        String correlationId = generateCorrelationId();
        String userId = authentication.getName();
        
        MDC.put("correlationId", correlationId);
        MDC.put("userId", userId);
        MDC.put("accountId", accountId);
        MDC.put("operation", "import-transactions");
        
        try {
            logger.info("Starting bank transaction import for account: {} by user: {}", accountId, userId);
            
            ImportBankTransactionsWebResponse response = ynabSyncApplicationService.importBankTransactions(accountId, request, authentication);
            
            logger.info("Bank transaction import completed successfully for user: {}. Total: {}, Successful: {}, Failed: {}",
                    userId, response.totalTransactions(), response.successfulImports(), response.failedImports());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to import bank transactions for account: {}", accountId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Reconciles transactions between YNAB and bank account for a specific date range.
     * 
     * Business Operation: Match bank transactions with existing YNAB transactions
     * Workflow Position: Step 2 of YNAB-Bank reconciliation process
     * 
     * Security: Requires USER role or higher (ADMIN also allowed)
     * Phase 3: Account-level authorization - user must own the account
     * 
     * @param accountId the bank account identifier
     * @param request the reconciliation request containing date range and strategy
     * @param authentication the current user's authentication details
     * @return the reconciliation response with matched and missing transactions
     */
    @PostMapping("/accounts/{accountId}/reconcile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReconcileTransactionsWebResponse> reconcileTransactions(
            @PathVariable @ValidAccountId String accountId,
            @Valid @RequestBody ReconcileTransactionsWebRequest request,
            Authentication authentication) {
        
        String correlationId = generateCorrelationId();
        String userId = authentication.getName();
        
        MDC.put("correlationId", correlationId);
        MDC.put("userId", userId);
        MDC.put("accountId", accountId);
        MDC.put("operation", "reconcile-transactions");
        
        try {
            logger.info("Starting transaction reconciliation for account: {} from {} to {} by user: {}", 
                    accountId, request.fromDate(), request.toDate(), userId);
            
            ReconcileTransactionsWebResponse response = ynabSyncApplicationService.reconcileTransactions(accountId, request, authentication);
            
            logger.info("Transaction reconciliation completed for account {} by user {}: {} matched, {} missing from YNAB",
                    accountId, userId, response.matchedTransactions(), response.missingFromYnab());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to reconcile transactions for account: {} by user: {}", accountId, userId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Infers categories for transactions using ML-based analysis.
     * 
     * Business Operation: ML-powered category recommendation for bank transactions
     * Workflow Position: Optional step for enhanced categorization
     * 
     * Security: Requires READ_ONLY role or higher (read-only operation)
     * Phase 3: Account-level authorization - user must own the account
     * 
     * @param accountId the bank account identifier
     * @param request the inference request containing transactions to categorize
     * @param authentication the current user's authentication details
     * @return the inference response with category suggestions and confidence scores
     */
    @PostMapping("/accounts/{accountId}/transactions/infer-categories")
    @PreAuthorize("hasRole('READ_ONLY') or hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<InferCategoriesWebResponse> inferCategories(
            @PathVariable @ValidAccountId String accountId,
            @Valid @RequestBody InferCategoriesWebRequest request,
            Authentication authentication) {
        
        String correlationId = generateCorrelationId();
        String userId = authentication.getName();
        
        MDC.put("correlationId", correlationId);
        MDC.put("userId", userId);
        MDC.put("accountId", accountId);
        MDC.put("operation", "infer-categories");
        
        try {
            logger.info("Starting category inference for account: {} with {} transactions by user: {}", 
                    accountId, request.transactions().size(), userId);
            
            InferCategoriesWebResponse response = ynabSyncApplicationService.inferCategories(accountId, request, authentication);
            
            logger.info("Category inference completed for account {} by user {}: {} categories inferred, {} low confidence",
                    accountId, userId, response.categoriesInferred(), response.lowConfidenceResults());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to infer categories for account: {} by user: {}", accountId, userId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Creates missing transactions in YNAB that were identified during reconciliation.
     * 
     * Business Operation: Create YNAB transactions for unmatched bank transactions
     * Workflow Position: Step 3 of YNAB-Bank reconciliation process
     * 
     * Security: Requires USER role or higher (creates data in YNAB)
     * Phase 3: Account-level authorization - user must own the account
     * 
     * @param accountId the bank account identifier
     * @param request the creation request containing missing transaction data
     * @param authentication the current user's authentication details
     * @return the creation response with results and statistics
     */
    @PostMapping("/accounts/{accountId}/transactions/create-missing")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CreateMissingTransactionsWebResponse> createMissingTransactions(
            @PathVariable @ValidAccountId String accountId,
            @Valid @RequestBody CreateMissingTransactionsWebRequest request,
            Authentication authentication) {
        
        String correlationId = generateCorrelationId();
        String userId = authentication.getName();
        
        MDC.put("correlationId", correlationId);
        MDC.put("userId", userId);
        MDC.put("accountId", accountId);
        MDC.put("operation", "create-missing-transactions");
        
        try {
            logger.info("Starting missing transaction creation for account: {} with {} transactions by user: {}", 
                    accountId, request.missingTransactions().size(), userId);
            
            CreateMissingTransactionsWebResponse response = ynabSyncApplicationService.createMissingTransactions(accountId, request, authentication);
            
            logger.info("Missing transaction creation completed for account {} by user {}: {} successful, {} failed",
                    accountId, userId, response.successfulCreations(), response.failedCreations());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to create missing transactions for account: {} by user: {}", accountId, userId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Saves learned category mappings to improve ML inference accuracy.
     * 
     * Business Operation: Global ML knowledge base management (cross-account)
     * Workflow Position: ML model improvement and learning phase
     * 
     * Security: Requires ADMIN role (affects global ML model)
     * 
     * @param request the save request containing category mappings to persist
     * @return the save response with persistence results and statistics
     */
    @PostMapping("/category-mappings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SaveCategoryMappingsWebResponse> saveCategoryMappings(
            @Valid @RequestBody SaveCategoryMappingsWebRequest request) {
        
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Starting category mapping save operation with {} mappings", 
                    request.categoryMappings().size());
            
            SaveCategoryMappingsWebResponse response = ynabSyncApplicationService.saveCategoryMappings(request);
            
            logger.info("Category mapping save completed: {} new, {} updated, {} skipped, status: {}",
                    response.savedNew(), response.updatedExisting(), response.skipped(), response.status());
            
            return ResponseEntity.ok()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Failed to save category mappings", e);
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