package co.personal.ynabsyncher.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing account ownership and permissions.
 * 
 * Maps the relationship between users and their financial accounts,
 * supporting fine-grained access control and audit trails.
 * 
 * Phase 3: Account-Level Authorization
 */
@Entity
@Table(name = "account_ownership", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_account_ownership_user_account", 
           columnNames = {"user_id", "account_id"}
       ),
       indexes = {
           @Index(name = "idx_account_ownership_user_id", columnList = "user_id"),
           @Index(name = "idx_account_ownership_account_id", columnList = "account_id"),
           @Index(name = "idx_account_ownership_permission", columnList = "permission")
       })
public class AccountOwnership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;
    
    @Column(name = "account_id", nullable = false, length = 255) 
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 20)
    private AccountPermission permission;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
    
    // Default constructor for JPA
    protected AccountOwnership() {}
    
    /**
     * Creates a new account ownership record.
     * 
     * @param userId the user who owns/has access to the account
     * @param accountId the financial account identifier
     * @param permission the level of access granted
     * @param createdBy the user/system that created this ownership record
     */
    public AccountOwnership(String userId, String accountId, AccountPermission permission, String createdBy) {
        this.userId = userId;
        this.accountId = accountId;
        this.permission = permission;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }
    
    /**
     * Updates the permission level for this account ownership.
     * 
     * @param newPermission the new permission level
     * @param updatedBy the user/system making the update
     */
    public void updatePermission(AccountPermission newPermission, String updatedBy) {
        this.permission = newPermission;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public AccountPermission getPermission() {
        return permission;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountOwnership that = (AccountOwnership) o;
        return Objects.equals(userId, that.userId) && 
               Objects.equals(accountId, that.accountId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, accountId);
    }
    
    @Override
    public String toString() {
        return "AccountOwnership{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", permission=" + permission +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}

/**
 * Enumeration of account permission levels.
 * 
 * Defines the hierarchy of access permissions that can be granted
 * to users for specific financial accounts.
 */
enum AccountPermission {
    /**
     * Full ownership with all permissions including delegation and deletion.
     */
    OWNER,
    
    /**
     * Read and write access to account data and transactions.
     */
    READ_WRITE,
    
    /**
     * Read-only access to account data for viewing and reporting.
     */
    READ_ONLY
}