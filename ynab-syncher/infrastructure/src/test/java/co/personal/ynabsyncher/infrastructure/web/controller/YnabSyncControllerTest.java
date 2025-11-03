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
import co.personal.ynabsyncher.infrastructure.web.dto.BankTransactionWebData;
import co.personal.ynabsyncher.infrastructure.web.dto.CategoryMappingWebData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for YnabSyncController following Netflix/Uber pattern.
 * 
 * Tests HTTP protocol concerns only:
 * - Request/response mapping
 * - Status codes and headers
 * - Bean validation
 * - Path variable binding
 * - Correlation ID propagation
 * 
 * Architectural Validation:
 * - Controller only depends on application service (not domain use cases)
 * - HTTP concerns are isolated from business logic
 * - Proper DTO usage at boundaries
 */
@WebMvcTest(YnabSyncController.class)
@DisplayName("YNAB Sync Controller Tests - HTTP Protocol Concerns")
class YnabSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private YnabSyncApplicationService ynabSyncApplicationService;

    @Test
    @DisplayName("Should import bank transactions successfully with correlation ID")
    void shouldImportBankTransactionsSuccessfully() throws Exception {
        // Given
        ImportBankTransactionsWebRequest request = new ImportBankTransactionsWebRequest(
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant"))
        );
        
        ImportBankTransactionsWebResponse response = new ImportBankTransactionsWebResponse(
                1, 1, 0, List.of(), List.of("Import completed successfully")
        );

        when(ynabSyncApplicationService.importBankTransactions(eq("account-123"), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalTransactions").value(1))
                .andExpect(jsonPath("$.successfulImports").value(1))
                .andExpect(jsonPath("$.failedImports").value(0));
    }

    @Test
    @DisplayName("Should return 400 for invalid import request")
    void shouldReturn400ForInvalidImportRequest() throws Exception {
        // Given - Invalid request with empty transactions list
        ImportBankTransactionsWebRequest request = new ImportBankTransactionsWebRequest(
                List.of() // Empty transactions list should fail validation
        );

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    @DisplayName("Should reconcile transactions successfully")
    void shouldReconcileTransactionsSuccessfully() throws Exception {
        // Given
        ReconcileTransactionsWebRequest request = new ReconcileTransactionsWebRequest(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "STRICT"
        );
        
        ReconcileTransactionsWebResponse response = new ReconcileTransactionsWebResponse(
                10, 10, 8, 2, 0, List.of(), List.of(), List.of()
        );

        when(ynabSyncApplicationService.reconcileTransactions(eq("account-123"), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalBankTransactions").value(10))
                .andExpect(jsonPath("$.matchedTransactions").value(8))
                .andExpect(jsonPath("$.missingFromYnab").value(2));
    }

    @Test
    @DisplayName("Should return 400 for invalid reconciliation strategy")
    void shouldReturn400ForInvalidReconciliationStrategy() throws Exception {
        // Given - Invalid strategy
        ReconcileTransactionsWebRequest request = new ReconcileTransactionsWebRequest(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "INVALID_STRATEGY"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }

    @Test
    @DisplayName("Should infer categories successfully")
    void shouldInferCategoriesSuccessfully() throws Exception {
        // Given
        InferCategoriesWebRequest request = new InferCategoriesWebRequest(
                "budget-123",
                List.of(new BankTransactionWebData("2024-01-15", "Starbucks Coffee", "-5.25", "Starbucks"))
        );
        
        InferCategoriesWebResponse response = InferCategoriesWebResponse.success(
                1, 1, 0, List.of()
        );

        when(ynabSyncApplicationService.inferCategories(eq("account-123"), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/infer-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalTransactions").value(1))
                .andExpect(jsonPath("$.categoriesInferred").value(1))
                .andExpect(jsonPath("$.lowConfidenceResults").value(0));
    }

    @Test
    @DisplayName("Should create missing transactions successfully")
    void shouldCreateMissingTransactionsSuccessfully() throws Exception {
        // Given
        CreateMissingTransactionsWebRequest request = new CreateMissingTransactionsWebRequest(
                "budget-123",
                "ynab-account-456",
                List.of(new BankTransactionWebData("2024-01-15", "Missing transaction", "75.00", "Test Merchant"))
        );
        
        CreateMissingTransactionsWebResponse response = new CreateMissingTransactionsWebResponse(
                1, 1, 0, List.of("txn-789"), List.of()
        );

        when(ynabSyncApplicationService.createMissingTransactions(eq("account-123"), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/create-missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalMissingTransactions").value(1))
                .andExpect(jsonPath("$.successfulCreations").value(1))
                .andExpect(jsonPath("$.failedCreations").value(0));
    }

    @Test
    @DisplayName("Should return 400 for empty missing transactions list")
    void shouldReturn400ForEmptyMissingTransactionsList() throws Exception {
        // Given - Empty transactions list
        CreateMissingTransactionsWebRequest request = new CreateMissingTransactionsWebRequest(
                "budget-123",
                "ynab-account-456",
                List.of() // Empty list
        );

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/create-missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }

    @Test
    @DisplayName("Should save category mappings successfully (global operation)")
    void shouldSaveCategoryMappingsSuccessfully() throws Exception {
        // Given
        SaveCategoryMappingsWebRequest request = new SaveCategoryMappingsWebRequest(
                List.of(new CategoryMappingWebData(
                        "mapping-123",
                        "category-456",
                        "Dining",
                        List.of("starbucks", "coffee", "restaurant"),
                        BigDecimal.valueOf(0.95),
                        "ML_INFERENCE"
                ))
        );
        
        SaveCategoryMappingsWebResponse response = SaveCategoryMappingsWebResponse.success(
                1, 1, 0, List.of()
        );

        when(ynabSyncApplicationService.saveCategoryMappings(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/category-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalMappings").value(1))
                .andExpect(jsonPath("$.savedNew").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should return 400 for empty category mappings list")
    void shouldReturn400ForEmptyCategoryMappingsList() throws Exception {
        // Given - Empty mappings list
        SaveCategoryMappingsWebRequest request = new SaveCategoryMappingsWebRequest(
                List.of() // Empty list
        );

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/category-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }

    @Test
    @DisplayName("Should handle application service exceptions properly")
    void shouldHandleApplicationServiceExceptionsProperly() throws Exception {
        // Given
        ImportBankTransactionsWebRequest request = new ImportBankTransactionsWebRequest(
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant"))
        );

        when(ynabSyncApplicationService.importBankTransactions(eq("account-123"), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(post("/api/v1/reconciliation/accounts/account-123/transactions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Correlation-ID"));
    }
}