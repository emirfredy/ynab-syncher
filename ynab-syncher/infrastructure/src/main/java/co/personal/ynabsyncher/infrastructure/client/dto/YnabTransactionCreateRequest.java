package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Transaction create request wrapper.
 */
public class YnabTransactionCreateRequest {
    private YnabTransactionDto transaction;
    
    public YnabTransactionCreateRequest(YnabTransactionDto transaction) {
        this.transaction = transaction;
    }
    
    public YnabTransactionDto getTransaction() { return transaction; }
    public void setTransaction(YnabTransactionDto transaction) { this.transaction = transaction; }
}