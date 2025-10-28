package co.personal.ynabsyncher.api.dto;

/**
 * Enumeration representing the overall result of an import operation.
 */
public enum ImportResult {
    
    /**
     * All transactions were successfully processed.
     */
    SUCCESS,
    
    /**
     * Some transactions were processed successfully, but others failed or were skipped.
     */
    PARTIAL_SUCCESS,
    
    /**
     * No transactions were successfully processed.
     */
    FAILED
}