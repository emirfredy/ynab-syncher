package co.personal.ynabsyncher.infrastructure.security;

import co.personal.ynabsyncher.infrastructure.persistence.AccountOwnershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Service responsible for account-level access control validation.
 * 
 * Implements fine-grained authorization to ensure users can only access
 * their own financial accounts and resources, with hierarchical role support.
 * 
 * Security Model:
 * - ADMIN: Can access any account (system administrator)
 * - USER/READ_ONLY: Can only access accounts they own
 * - Account ownership is validated against the account_ownership table
 * 
 * Phase 3: Account-Level Authorization
 */
@Service
public class AccountAccessControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountAccessControlService.class);
    
    private final AccountOwnershipRepository accountOwnershipRepository;
    
    public AccountAccessControlService(AccountOwnershipRepository accountOwnershipRepository) {
        this.accountOwnershipRepository = accountOwnershipRepository;
    }
    
    /**
     * Validates if the authenticated user has access to the specified account.
     * Supports hierarchical access: ADMIN can access any account, USER can access owned accounts.
     * 
     * @param userId the authenticated user's ID
     * @param accountId the account ID being accessed
     * @param userRoles the user's assigned roles
     * @return true if access is granted, false otherwise
     */
    public boolean hasAccountAccess(String userId, String accountId, Collection<String> userRoles) {
        if (userId == null || accountId == null || userRoles == null) {
            logger.warn("Invalid parameters for account access check: userId={}, accountId={}, userRoles={}", 
                       userId, accountId, userRoles);
            return false;
        }
        
        // ADMIN role bypasses account ownership checks
        if (userRoles.contains("ROLE_ADMIN")) {
            logger.debug("ADMIN user {} granted access to account {}", userId, accountId);
            return true;
        }
        
        // For USER/READ_ONLY roles, verify account ownership
        boolean hasAccess = accountOwnershipRepository.isAccountOwnedByUser(userId, accountId);
        
        if (hasAccess) {
            logger.debug("User {} has verified ownership of account {}", userId, accountId);
        } else {
            logger.warn("User {} denied access to account {} - not an owner", userId, accountId);
        }
        
        return hasAccess;
    }
    
    /**
     * Validates account access for specific operations with operation-level permissions.
     * Provides extensibility for future fine-grained permission models.
     * 
     * @param userId the authenticated user's ID
     * @param accountId the account ID being accessed
     * @param operation the operation being performed (READ, WRITE, ADMIN)
     * @param userRoles the user's assigned roles
     * @return true if access is granted for the specific operation, false otherwise
     */
    public boolean hasAccountAccess(String userId, String accountId, String operation, Collection<String> userRoles) {
        // First check basic account access
        if (!hasAccountAccess(userId, accountId, userRoles)) {
            return false;
        }
        
        // Then validate operation-specific permissions
        boolean hasOperationAccess = switch (operation) {
            case "READ" -> userRoles.contains("ROLE_READ_ONLY") || 
                          userRoles.contains("ROLE_USER") || 
                          userRoles.contains("ROLE_ADMIN");
            case "WRITE" -> userRoles.contains("ROLE_USER") || 
                           userRoles.contains("ROLE_ADMIN");
            case "ADMIN" -> userRoles.contains("ROLE_ADMIN");
            default -> {
                logger.warn("Unknown operation '{}' requested by user {} for account {}", operation, userId, accountId);
                yield false;
            }
        };
        
        if (!hasOperationAccess) {
            logger.warn("User {} with roles {} denied '{}' access to account {}", 
                       userId, userRoles, operation, accountId);
        }
        
        return hasOperationAccess;
    }
}