package co.personal.ynabsyncher.api.usecase;

import co.personal.ynabsyncher.model.reconciliation.ReconciliationRequest;
import co.personal.ynabsyncher.model.reconciliation.ReconciliationResult;

/**
 * Use case interface for reconciling transactions between YNAB and bank accounts.
 * This is an inbound port in the hexagonal architecture.
 */
public interface ReconcileTransactions {

    /**
     * Reconciles transactions between YNAB and bank for a specific account and date range.
     * 
     * @param request the reconciliation request containing account and date range
     * @return the reconciliation result showing matched and missing transactions
     */
    ReconciliationResult reconcile(ReconciliationRequest request);
}