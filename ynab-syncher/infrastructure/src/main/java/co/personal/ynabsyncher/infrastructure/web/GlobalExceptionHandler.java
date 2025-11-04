package co.personal.ynabsyncher.infrastructure.web;

import co.personal.ynabsyncher.api.error.YnabApiException;
import co.personal.ynabsyncher.infrastructure.exception.AccountAccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Maps domain exceptions to appropriate HTTP responses using Problem Details JSON format.
 * Ensures consistent error responses and correlation ID propagation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * Handles domain validation errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(IllegalArgumentException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("/errors/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles YNAB API exceptions.
     */
    @ExceptionHandler(YnabApiException.class)
    public ResponseEntity<ProblemDetail> handleYnabApiException(YnabApiException ex) {
        logger.error("YNAB API exception: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("YNAB API Error");
        problemDetail.setType(URI.create("/errors/ynab-api-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorId", ex.getErrorId());
        problemDetail.setProperty("errorName", ex.getErrorName());
        problemDetail.setProperty("errorDetail", ex.getErrorDetail());
        
        return ResponseEntity.status(status)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles Bean Validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Bean validation errors: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setTitle("Request Validation Error");
        problemDetail.setType(URI.create("/errors/request-validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles Spring Boot 3.5+ handler method validation errors.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        logger.warn("Handler method validation errors: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setTitle("Request Validation Error");
        problemDetail.setType(URI.create("/errors/request-validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("validationDetail", ex.getReason());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles Spring Security authorization exceptions (403 Forbidden).
     * These should not be caught by the generic exception handler.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles account access denied exceptions from Phase 3 authorization.
     * These indicate that a user attempted to access an account they don't own.
     */
    @ExceptionHandler(AccountAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccountAccessDenied(AccountAccessDeniedException ex) {
        String correlationId = getCorrelationId();
        
        // Log with structured information for security auditing
        if (ex.getUserId() != null && ex.getAccountId() != null) {
            logger.warn("Account access denied: user '{}' attempted '{}' on account '{}' [correlationId={}]",
                       ex.getUserId(), ex.getOperation(), ex.getAccountId(), correlationId);
        } else {
            logger.warn("Account access denied: {} [correlationId={}]", ex.getMessage(), correlationId);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not have permission to access this account");
        problemDetail.setTitle("Account Access Denied");
        problemDetail.setType(URI.create("/errors/account-access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("correlationId", correlationId);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }

    /**
     * Handles Spring Security authentication exceptions (401 Unauthorized).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication required");
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create("/errors/authentication-required"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Handles unexpected internal errors.
     * NOTE: Spring Security exceptions are handled above and should not reach this handler.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleInternalError(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CORRELATION_ID_HEADER, getCorrelationId())
                .body(problemDetail);
    }

    /**
     * Gets correlation ID from MDC context.
     */
    private String getCorrelationId() {
        return MDC.get("correlationId");
    }
}