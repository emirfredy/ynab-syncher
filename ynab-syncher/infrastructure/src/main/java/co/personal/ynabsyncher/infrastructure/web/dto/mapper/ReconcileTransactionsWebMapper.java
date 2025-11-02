package co.personal.ynabsyncher.infrastructure.web.dto.mapper;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationStrategy;
import co.personal.ynabsyncher.infrastructure.web.dto.BankTransactionWebData;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebRequest;
import co.personal.ynabsyncher.infrastructure.web.dto.ReconcileTransactionsWebResponse;
import co.personal.ynabsyncher.infrastructure.web.dto.TransactionMatchWebData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Web DTOs and Domain DTOs for transaction reconciliation.
 */
@Component
public class ReconcileTransactionsWebMapper {

    /**
     * Converts web request to domain request for transaction reconciliation.
     */
    public ReconciliationRequest toDomainRequest(ReconcileTransactionsWebRequest webRequest) {
        return ReconciliationRequest.of(
                AccountId.of(webRequest.accountId()),
                webRequest.fromDate(),
                webRequest.toDate(),
                ReconciliationStrategy.valueOf(webRequest.reconciliationStrategy())
        );
    }

    /**
     * Converts domain response to web response for transaction reconciliation.
     */
    public ReconcileTransactionsWebResponse toWebResponse(ReconciliationResult domainResult) {
        return new ReconcileTransactionsWebResponse(
                domainResult.summary().totalBankTransactions(),
                domainResult.summary().totalYnabTransactions(),
                domainResult.getMatchedCount(),
                domainResult.missingFromYnab().size(),
                0, // Missing from bank - not currently tracked in domain
                convertToTransactionMatches(domainResult),
                convertToWebBankTransactions(domainResult.missingFromYnab()),
                List.of() // Missing from bank - would need additional domain data
        );
    }

    private List<TransactionMatchWebData> convertToTransactionMatches(ReconciliationResult domainResult) {
        // For now, return empty list as we need more domain structure for transaction matches
        // This would be enhanced when full domain match details are available
        return List.of();
    }

    private List<BankTransactionWebData> convertToWebBankTransactions(List<co.personal.ynabsyncher.model.bank.BankTransaction> bankTransactions) {
        return bankTransactions.stream()
                .map(this::convertToWebBankTransaction)
                .toList();
    }

    private BankTransactionWebData convertToWebBankTransaction(co.personal.ynabsyncher.model.bank.BankTransaction bankTransaction) {
        return new BankTransactionWebData(
                bankTransaction.date().toString(),
                bankTransaction.description(),
                bankTransaction.amount().toString(),
                bankTransaction.merchantName()
        );
    }
}