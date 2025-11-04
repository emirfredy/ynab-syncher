package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.infrastructure.persistence.entity.AccountOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing account ownership and access control data.
 * 
 * Provides efficient queries for validating user permissions on financial accounts,
 * supporting the account-level authorization security model.
 * 
 * Performance Considerations:
 * - Optimized indexes on user_id and account_id columns
 * - Efficient EXISTS queries for permission checks
 * - Batch operations for bulk permission management
 * 
 * Phase 3: Account-Level Authorization
 */
@Repository
public interface AccountOwnershipRepository extends JpaRepository<AccountOwnership, String> {
    
    /**
     * Validates if the specified account belongs to the user.
     * Uses EXISTS for optimal performance.
     * 
     * @param userId the user identifier to check
     * @param accountId the account identifier to validate
     * @return true if the user owns/has access to the account
     */
    @Query("SELECT COUNT(a) > 0 FROM AccountOwnership a WHERE a.accountId = :accountId AND a.userId = :userId")
    boolean isAccountOwnedByUser(@Param("userId") String userId, @Param("accountId") String accountId);
    
    /**
     * Retrieves all accounts accessible by the user.
     * Used for admin operations and user account listings.
     * 
     * @param userId the user identifier
     * @return list of account IDs accessible to the user
     */
    @Query("SELECT a.accountId FROM AccountOwnership a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<String> findAccountsByUserId(@Param("userId") String userId);
    
    /**
     * Validates if user has specific permissions on an account.
     * Supports fine-grained permission checking for future extensibility.
     * 
     * @param userId the user identifier
     * @param accountId the account identifier
     * @param permissions list of acceptable permission levels
     * @return true if user has any of the specified permissions
     */
    @Query("SELECT COUNT(a) > 0 FROM AccountOwnership a WHERE a.accountId = :accountId AND a.userId = :userId AND a.permission IN :permissions")
    boolean hasAccountPermission(@Param("userId") String userId, 
                                @Param("accountId") String accountId, 
                                @Param("permissions") List<String> permissions);
    
    /**
     * Finds the specific ownership record for a user-account combination.
     * Used for permission updates and detailed access control.
     * 
     * @param userId the user identifier
     * @param accountId the account identifier
     * @return the ownership record if found
     */
    @Query("SELECT a FROM AccountOwnership a WHERE a.userId = :userId AND a.accountId = :accountId")
    Optional<AccountOwnership> findByUserIdAndAccountId(@Param("userId") String userId, @Param("accountId") String accountId);
    
    /**
     * Retrieves all ownership records for a specific account.
     * Used for account access management and audit purposes.
     * 
     * @param accountId the account identifier
     * @return list of all ownership records for the account
     */
    @Query("SELECT a FROM AccountOwnership a WHERE a.accountId = :accountId ORDER BY a.permission DESC, a.createdAt ASC")
    List<AccountOwnership> findByAccountId(@Param("accountId") String accountId);
    
    /**
     * Counts the number of accounts accessible to a user.
     * Used for dashboard statistics and access validation.
     * 
     * @param userId the user identifier
     * @return count of accessible accounts
     */
    @Query("SELECT COUNT(a) FROM AccountOwnership a WHERE a.userId = :userId")
    long countAccountsByUserId(@Param("userId") String userId);
    
    /**
     * Checks if an account has any ownership records.
     * Used for validation before account operations.
     * 
     * @param accountId the account identifier
     * @return true if the account has at least one ownership record
     */
    @Query("SELECT COUNT(a) > 0 FROM AccountOwnership a WHERE a.accountId = :accountId")
    boolean hasAnyOwnership(@Param("accountId") String accountId);
}