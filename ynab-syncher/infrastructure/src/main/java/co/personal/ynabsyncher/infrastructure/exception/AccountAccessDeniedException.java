package co.personal.ynabsyncher.infrastructure.exception;

/**
 * Exception thrown when a user attempts to access an account they don't own
 * or don't have sufficient permissions for.
 * 
 * This exception represents a security violation and should be handled carefully
 * to avoid information disclosure while maintaining audit trails.
 * 
 * Phase 3: Account-Level Authorization
 */
public class AccountAccessDeniedException extends RuntimeException {
    
    private final String userId;
    private final String accountId;
    private final String operation;
    
    /**
     * Creates an exception with a custom message.
     * 
     * @param message the error message
     */
    public AccountAccessDeniedException(String message) {
        super(message);
        this.userId = null;
        this.accountId = null;
        this.operation = null;
    }
    
    /**
     * Creates an exception with structured details for auditing.
     * 
     * @param userId the user who attempted access
     * @param accountId the account that was accessed
     * @param operation the operation that was attempted
     */
    public AccountAccessDeniedException(String userId, String accountId, String operation) {
        super(String.format("User '%s' does not have '%s' access to account '%s'", userId, operation, accountId));
        this.userId = userId;
        this.accountId = accountId;
        this.operation = operation;
    }
    
    /**
     * Creates an exception with structured details and custom message.
     * 
     * @param message custom error message
     * @param userId the user who attempted access
     * @param accountId the account that was accessed
     * @param operation the operation that was attempted
     */
    public AccountAccessDeniedException(String message, String userId, String accountId, String operation) {
        super(message);
        this.userId = userId;
        this.accountId = accountId;
        this.operation = operation;
    }
    
    /**
     * Gets the user ID for audit logging.
     * 
     * @return the user ID or null if not specified
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the account ID for audit logging.
     * 
     * @return the account ID or null if not specified
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Gets the attempted operation for audit logging.
     * 
     * @return the operation or null if not specified
     */
    public String getOperation() {
        return operation;
    }
}