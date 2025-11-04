package co.personal.ynabsyncher.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for account ID format and security.
 * 
 * Ensures account IDs meet security requirements:
 * - Proper format (alphanumeric, underscore, hyphen only)
 * - Length constraints (8-64 characters)
 * - No path traversal attempts
 * - No script injection patterns
 * 
 * Phase 3: Account-Level Authorization
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountIdValidator.class)
public @interface ValidAccountId {
    String message() default "Invalid account ID format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator implementation for account ID security validation.
 * 
 * Validates that account IDs are safe for use in URLs and database queries,
 * preventing common injection and traversal attacks.
 */
@Component
class AccountIdValidator implements ConstraintValidator<ValidAccountId, String> {
    
    private static final String VALID_ACCOUNT_ID_PATTERN = "^[a-zA-Z0-9_-]{8,64}$";
    private static final String[] FORBIDDEN_PATTERNS = {
        "../", "./", "\\", "<script", "</script", "javascript:", "data:", "vbscript:",
        "onload=", "onerror=", "onclick=", "eval(", "alert(", "document.", "window.",
        "location.", "href=", "src=", "style=", "expression(", "url(", "@import",
        "DROP", "DELETE", "INSERT", "UPDATE", "CREATE", "ALTER", "UNION", "SELECT",
        "--", "/*", "*/", "xp_", "sp_", "exec", "execute", "char(", "ascii(",
        "substring(", "waitfor", "delay", "benchmark(", "sleep(", "pg_sleep("
    };
    
    @Override
    public void initialize(ValidAccountId constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String accountId, ConstraintValidatorContext context) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        
        // Check basic format requirements
        if (!accountId.matches(VALID_ACCOUNT_ID_PATTERN)) {
            return false;
        }
        
        // Check for security threats
        String lowerCaseAccountId = accountId.toLowerCase();
        for (String forbiddenPattern : FORBIDDEN_PATTERNS) {
            if (lowerCaseAccountId.contains(forbiddenPattern.toLowerCase())) {
                return false;
            }
        }
        
        // Additional checks for common attack patterns
        if (containsSuspiciousPatterns(lowerCaseAccountId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks for additional suspicious patterns that might indicate malicious intent.
     * 
     * @param accountId the account ID to check (already lowercase)
     * @return true if suspicious patterns are found
     */
    private boolean containsSuspiciousPatterns(String accountId) {
        // Check for excessive repetition of characters (potential buffer overflow)
        if (hasExcessiveRepetition(accountId)) {
            return true;
        }
        
        // Check for encoded patterns
        if (accountId.contains("%") || accountId.contains("&#") || accountId.contains("&amp;")) {
            return true;
        }
        
        // Check for null bytes or control characters
        return accountId.chars().anyMatch(c -> c < 32 || c == 127);
    }
    
    /**
     * Detects excessive character repetition that might indicate an attack.
     * 
     * @param accountId the account ID to check
     * @return true if excessive repetition is found
     */
    private boolean hasExcessiveRepetition(String accountId) {
        if (accountId.length() < 4) {
            return false;
        }
        
        int maxRepetition = 0;
        int currentRepetition = 1;
        char previousChar = accountId.charAt(0);
        
        for (int i = 1; i < accountId.length(); i++) {
            char currentChar = accountId.charAt(i);
            if (currentChar == previousChar) {
                currentRepetition++;
                maxRepetition = Math.max(maxRepetition, currentRepetition);
            } else {
                currentRepetition = 1;
            }
            previousChar = currentChar;
        }
        
        // Flag if more than 4 consecutive identical characters
        return maxRepetition > 4;
    }
}