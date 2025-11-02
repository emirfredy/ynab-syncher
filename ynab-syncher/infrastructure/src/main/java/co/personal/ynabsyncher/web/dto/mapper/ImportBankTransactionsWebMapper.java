package co.personal.ynabsyncher.web.dto.mapper;

import co.personal.ynabsyncher.api.dto.BankTransactionData;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsRequest;
import co.personal.ynabsyncher.api.dto.ImportBankTransactionsResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.BankTransactionWebData;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ImportBankTransactionsWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Web DTOs and Domain DTOs for bank transaction import.
 * Follows Netflix/Uber pattern for DTO conversion at application service boundaries.
 */
@Component
public class ImportBankTransactionsWebMapper {

    /**
     * Converts web request to domain request for bank transaction import.
     * 
     * @param webRequest the web layer request
     * @return domain layer request
     */
    public ImportBankTransactionsRequest toDomainRequest(ImportBankTransactionsWebRequest webRequest) {
        List<BankTransactionData> domainTransactions = webRequest.transactions().stream()
                .map(this::toDomainTransactionData)
                .toList();
        
        return new ImportBankTransactionsRequest(
                webRequest.accountId(),
                domainTransactions
        );
    }

    /**
     * Converts domain response to web response for bank transaction import.
     * 
     * @param domainResponse the domain layer response
     * @return web layer response
     */
    public ImportBankTransactionsWebResponse toWebResponse(ImportBankTransactionsResponse domainResponse) {
        return new ImportBankTransactionsWebResponse(
                domainResponse.totalProcessed(),
                domainResponse.successfulImports(),
                domainResponse.failedImports(),
                domainResponse.errors(),
                List.of("Import completed with result: " + domainResponse.result())
        );
    }

    /**
     * Converts web transaction data to domain transaction data.
     */
    private BankTransactionData toDomainTransactionData(BankTransactionWebData webData) {
        return new BankTransactionData(
                webData.date(),
                webData.description(),
                webData.amount(),
                webData.merchantName()
        );
    }
}