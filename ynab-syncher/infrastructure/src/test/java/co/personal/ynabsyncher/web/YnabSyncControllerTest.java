package co.personal.ynabsyncher.web;

import co.personal.ynabsyncher.infrastructure.service.YnabSyncApplicationService;
import co.personal.ynabsyncher.infrastructure.web.dto.BankTransactionWebData;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.SyncTransactionsWebResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for YnabSyncController focusing on HTTP protocol concerns.
 * Uses @WebMvcTest to test only the web layer with mocked application service.
 */
@WebMvcTest(co.personal.ynabsyncher.infrastructure.web.YnabSyncController.class)
class YnabSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private YnabSyncApplicationService ynabSyncApplicationService;

    @Test
    @DisplayName("Should import bank transactions and return 200 with correlation ID")
    void shouldImportBankTransactions() throws Exception {
        // Given
        ImportBankTransactionsWebRequest request = new ImportBankTransactionsWebRequest(
                "account-123",
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant"))
        );
        
        ImportBankTransactionsWebResponse response = new ImportBankTransactionsWebResponse(
                1, 1, 0, List.of(), List.of("Import completed with result: SUCCESS")
        );

        when(ynabSyncApplicationService.importBankTransactions(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/import")
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
        // Given - Invalid request with blank account ID
        ImportBankTransactionsWebRequest request = new ImportBankTransactionsWebRequest(
                "", // Blank account ID
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant"))
        );

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }

    @Test
    @DisplayName("Should reconcile transactions and return 200 with correlation ID")
    void shouldReconcileTransactions() throws Exception {
        // Given
        ReconcileTransactionsWebRequest request = new ReconcileTransactionsWebRequest(
                "account-123",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "STRICT"
        );
        
        ReconcileTransactionsWebResponse response = new ReconcileTransactionsWebResponse(
                10, 10, 8, 2, 0, List.of(), List.of(), List.of()
        );

        when(ynabSyncApplicationService.reconcileTransactions(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/reconcile")
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
                "account-123",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "INVALID_STRATEGY"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }

    @Test
    @DisplayName("Should sync transactions and return 200 when successful")
    void shouldSyncTransactionsSuccessfully() throws Exception {
        // Given
        SyncTransactionsWebRequest request = new SyncTransactionsWebRequest(
                "budget-123",
                "account-123",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "STRICT",
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant")),
                true
        );
        
        SyncTransactionsWebResponse response = new SyncTransactionsWebResponse(
                null, null, null, true, "SUCCESS", "Sync completed successfully"
        );

        when(ynabSyncApplicationService.syncTransactions(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.syncCompleted").value(true))
                .andExpect(jsonPath("$.overallStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should sync transactions and return 206 when partially successful")
    void shouldSyncTransactionsPartiallySuccessful() throws Exception {
        // Given
        SyncTransactionsWebRequest request = new SyncTransactionsWebRequest(
                "budget-123",
                "account-123",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "STRICT",
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant")),
                true
        );
        
        SyncTransactionsWebResponse response = new SyncTransactionsWebResponse(
                null, null, null, false, "PARTIAL_SUCCESS", "Sync completed with issues"
        );

        when(ynabSyncApplicationService.syncTransactions(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPartialContent())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.syncCompleted").value(false))
                .andExpect(jsonPath("$.overallStatus").value("PARTIAL_SUCCESS"));
    }

    @Test
    @DisplayName("Should return 400 when sync request has empty transactions list")
    void shouldReturn400ForEmptyTransactionsList() throws Exception {
        // Given - Empty transactions list
        SyncTransactionsWebRequest request = new SyncTransactionsWebRequest(
                "budget-123",
                "account-123",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "STRICT",
                List.of(), // Empty list
                true
        );

        // When & Then
        mockMvc.perform(post("/api/v1/ynab-sync/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.title").value("Request Validation Error"));
    }
}