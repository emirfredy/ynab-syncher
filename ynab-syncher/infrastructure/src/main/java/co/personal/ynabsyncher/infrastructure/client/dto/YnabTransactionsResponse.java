package co.personal.ynabsyncher.infrastructure.client.dto;

import java.util.List;

/**
 * YNAB API Transactions response wrapper.
 */
public class YnabTransactionsResponse {
    private YnabTransactionsData data;
    
    public YnabTransactionsData getData() { return data; }
    public void setData(YnabTransactionsData data) { this.data = data; }
    
    public static class YnabTransactionsData {
        private List<YnabTransactionDto> transactions;
        
        public List<YnabTransactionDto> getTransactions() { return transactions; }
        public void setTransactions(List<YnabTransactionDto> transactions) { this.transactions = transactions; }
    }
}