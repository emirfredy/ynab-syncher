package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Transaction update request wrapper.
 */
public class YnabTransactionUpdateRequest {
    private YnabTransactionDto transaction;
    
    public YnabTransactionUpdateRequest(YnabTransactionDto transaction) {
        this.transaction = transaction;
    }
    
    public YnabTransactionDto getTransaction() { return transaction; }
    public void setTransaction(YnabTransactionDto transaction) { this.transaction = transaction; }
}