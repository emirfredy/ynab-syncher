package co.personal.ynabsyncher.api.error;

/**
 * Exception thrown when YNAB API operations fail.
 * Contains specific error information from the YNAB API response.
 * 
 * This exception is part of the domain layer and contains all necessary
 * error information for proper error handling and user feedback.
 */
public class YnabApiException extends RuntimeException {
    private final int statusCode;
    private final String errorId;
    private final String errorName;
    private final String errorDetail;

    /**
     * Constructor for detailed YNAB API errors with specific error information.
     * 
     * @param message Human-readable error message
     * @param statusCode HTTP status code from the API response
     * @param errorId Unique error identifier from YNAB API
     * @param errorName Error category name from YNAB API
     * @param errorDetail Detailed error information from YNAB API
     */
    public YnabApiException(String message, int statusCode, String errorId, String errorName, String errorDetail) {
        super(message);
        this.statusCode = statusCode;
        this.errorId = errorId;
        this.errorName = errorName;
        this.errorDetail = errorDetail;
    }

    /**
     * Constructor for general API errors without specific YNAB error details.
     * Used for connection errors, parsing errors, and other non-API specific issues.
     * 
     * @param message Human-readable error message
     * @param cause The underlying cause of the error
     */
    public YnabApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorId = null;
        this.errorName = null;
        this.errorDetail = null;
    }

    /**
     * @return HTTP status code from the API response, or 0 if not available
     */
    public int getStatusCode() { 
        return statusCode; 
    }

    /**
     * @return Unique error identifier from YNAB API, or null if not available
     */
    public String getErrorId() { 
        return errorId; 
    }

    /**
     * @return Error category name from YNAB API, or null if not available
     */
    public String getErrorName() { 
        return errorName; 
    }

    /**
     * @return Detailed error information from YNAB API, or null if not available
     */
    public String getErrorDetail() { 
        return errorDetail; 
    }

    /**
     * @return true if this represents a client error (4xx status codes)
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * @return true if this represents a server error (5xx status codes)
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * @return true if this represents a rate limiting error (429 status code)
     */
    public boolean isRateLimited() {
        return statusCode == 429;
    }
}