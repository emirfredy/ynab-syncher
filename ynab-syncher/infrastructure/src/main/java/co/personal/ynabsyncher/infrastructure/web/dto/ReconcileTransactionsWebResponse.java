package co.personal.ynabsyncher.infrastructure.web.dto;

import java.util.List;

/**
 * Web response DTO for transaction reconciliation results.
 * Contains matched transactions and missing transactions from both sides.
 */
public record ReconcileTransactionsWebResponse(
        int totalBankTransactions,
        int totalYnabTransactions,
        int matchedTransactions,
        int missingFromYnab,
        int missingFromBank,
        List<TransactionMatchWebData> matches,
        List<BankTransactionWebData> missingFromYnabList,
        List<YnabTransactionWebData> missingFromBankList
) {
}