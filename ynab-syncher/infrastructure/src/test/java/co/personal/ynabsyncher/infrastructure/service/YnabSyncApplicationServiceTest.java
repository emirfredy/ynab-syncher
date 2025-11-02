package co.personal.ynabsyncher.infrastructure.service;

import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.api.dto.ImportResult;
import co.personal.ynabsyncher.api.usecase.ImportBankTransactions;
import co.personal.ynabsyncher.infrastructure.web.dto.*;
import co.personal.ynabsyncher.infrastructure.web.dto.mapper.ImportBankTransactionsWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for YnabSyncApplicationService following Netflix/Uber pattern.
 * 
 * Tests orchestration and DTO mapping concerns:
 * - DTO mapping between Web DTOs ↔ Domain DTOs
 * - Use case orchestration
 * - Cross-cutting concerns (transaction boundaries, etc.)
 * - Multi-step workflow coordination
 * 
 * Architectural Validation:
 * - Application service coordinates domain use cases
 * - Proper DTO transformation at boundaries
 * - Business orchestration without business logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("YNAB Sync Application Service Tests - Orchestration & DTO Mapping")
class YnabSyncApplicationServiceTest {

    @Mock
    private ImportBankTransactions importBankTransactions;
    
    @Mock
    private ImportBankTransactionsWebMapper importMapper;

    @InjectMocks
    private YnabSyncApplicationService applicationService;

    @Test
    @DisplayName("Should orchestrate import bank transactions with proper DTO mapping")
    void shouldOrchestrateImportBankTransactionsWithProperDtoMapping() {
        // Given
        ImportBankTransactionsWebRequest webRequest = new ImportBankTransactionsWebRequest(
                "account-123",
                List.of(new BankTransactionWebData("2024-01-15", "Test transaction", "100.00", "Test Merchant"))
        );
        
        ImportBankTransactionsRequest domainRequest = new ImportBankTransactionsRequest(
                "account-123",
                List.of(new co.personal.ynabsyncher.api.dto.BankTransactionData(
                        "2024-01-15", "Test transaction", "100.00", "Test Merchant"
                ))
        );
        
        ImportBankTransactionsResponse domainResponse = new ImportBankTransactionsResponse(
                ImportResult.SUCCESS,
                1, 1, 0,
                List.of(),
                List.of()
        );
        
        ImportBankTransactionsWebResponse expectedResponse = new ImportBankTransactionsWebResponse(
                1, 1, 0, List.of(), List.of("Import completed successfully")
        );

        when(importMapper.toDomainRequest(webRequest)).thenReturn(domainRequest);
        when(importBankTransactions.importTransactions(domainRequest)).thenReturn(domainResponse);
        when(importMapper.toWebResponse(domainResponse)).thenReturn(expectedResponse);

        // When
        ImportBankTransactionsWebResponse actualResponse = applicationService.importBankTransactions(webRequest);

        // Then
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(importMapper).toDomainRequest(webRequest);
        verify(importBankTransactions).importTransactions(domainRequest);
        verify(importMapper).toWebResponse(domainResponse);
    }
}