package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Transaction DTO for JSON serialization/deserialization.
 */
public class YnabTransactionDto {
    private String id;
    private String accountId;
    private String categoryId;
    private String categoryName;
    private String payeeName;
    private String date;
    private long amount;
    private String memo;
    private String cleared;
    private boolean approved;
    private String flagColor;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getCleared() { return cleared; }
    public void setCleared(String cleared) { this.cleared = cleared; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getFlagColor() { return flagColor; }
    public void setFlagColor(String flagColor) { this.flagColor = flagColor; }
}