package co.personal.ynabsyncher.infrastructure.web.dto.mapper;

import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsRequest;
import co.personal.ynabsyncher.api.dto.CreateMissingTransactionsResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.CreateMissingTransactionsWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Web DTO mapper for create missing transactions operations.
 * 
 * Converts between Web DTOs (optimized for HTTP/JSON) and Domain DTOs.
 * Follows hexagonal architecture by mapping across layer boundaries.
 */
@Component
public class CreateMissingTransactionsWebMapper {

    /**
     * Converts web request to domain request.
     * TODO: Implement proper domain mapping when BankTransaction domain model is available
     */
    public CreateMissingTransactionsRequest toDomainRequest(CreateMissingTransactionsWebRequest webRequest) {
        // Simplified implementation - needs proper domain mapping
        throw new UnsupportedOperationException("Domain mapping implementation needed");
    }

    /**
     * Converts domain response to web response.
     */
    public CreateMissingTransactionsWebResponse toWebResponse(CreateMissingTransactionsResponse domainResponse) {
        List<String> errors = domainResponse.getFailedResults().stream()
                .map(result -> result.errorMessage().orElse("Transaction creation failed"))
                .toList();

        return new CreateMissingTransactionsWebResponse(
                domainResponse.totalProcessed(),
                domainResponse.successfullyCreated(),
                domainResponse.failed(),
                domainResponse.getSuccessfulResults().stream()
                        .map(result -> result.ynabTransactionId().orElse(null))
                        .filter(Objects::nonNull)
                        .map(transactionId -> transactionId.value())
                        .toList(),
                errors
        );
    }
}