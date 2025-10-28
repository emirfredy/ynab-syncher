package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Account DTO for JSON serialization/deserialization.
 */
public class YnabAccountDto {
    private String id;
    private String name;
    private String type;
    private boolean onBudget;
    private boolean closed;
    private String note;
    private long balance;
    private long clearedBalance;
    private long unclearedBalance;
    private String transferPayeeId;
    private boolean directImportLinked;
    private String directImportInError;
    private String lastReconciledAt;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isOnBudget() { return onBudget; }
    public void setOnBudget(boolean onBudget) { this.onBudget = onBudget; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public long getClearedBalance() { return clearedBalance; }
    public void setClearedBalance(long clearedBalance) { this.clearedBalance = clearedBalance; }

    public long getUnclearedBalance() { return unclearedBalance; }
    public void setUnclearedBalance(long unclearedBalance) { this.unclearedBalance = unclearedBalance; }

    public String getTransferPayeeId() { return transferPayeeId; }
    public void setTransferPayeeId(String transferPayeeId) { this.transferPayeeId = transferPayeeId; }

    public boolean isDirectImportLinked() { return directImportLinked; }
    public void setDirectImportLinked(boolean directImportLinked) { this.directImportLinked = directImportLinked; }

    public String getDirectImportInError() { return directImportInError; }
    public void setDirectImportInError(String directImportInError) { this.directImportInError = directImportInError; }

    public String getLastReconciledAt() { return lastReconciledAt; }
    public void setLastReconciledAt(String lastReconciledAt) { this.lastReconciledAt = lastReconciledAt; }
}