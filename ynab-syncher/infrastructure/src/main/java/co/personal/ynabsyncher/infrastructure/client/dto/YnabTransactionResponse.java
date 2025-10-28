package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Transaction response wrapper.
 */
public class YnabTransactionResponse {
    private YnabTransactionData data;
    
    public YnabTransactionData getData() { return data; }
    public void setData(YnabTransactionData data) { this.data = data; }
    
    public static class YnabTransactionData {
        private YnabTransactionDto transaction;
        
        public YnabTransactionDto getTransaction() { return transaction; }
        public void setTransaction(YnabTransactionDto transaction) { this.transaction = transaction; }
    }
}