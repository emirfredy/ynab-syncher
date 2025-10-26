package co.personal.ynabsyncher.service;

import co.personal.ynabsyncher.model.bank.BankTransaction;
import co.personal.ynabsyncher.model.bank.BankTransactionAdapter;
import co.personal.ynabsyncher.model.matcher.TransactionMatcher;
import co.personal.ynabsyncher.model.reconciliation.*;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.model.ynab.YnabTransactionAdapter;

import java.time.LocalDate;
import java.util.*;

/**
 * Domain service responsible for the core transaction reconciliation algorithm.
 * Encapsulates matching logic and optimization strategies while maintaining business rules.
 */
public class TransactionReconciliationService {

    /**
     * Reconciles bank transactions against YNAB transactions using the specified matcher.
     * Implements date-ordered optimization to reduce matching complexity from O(n×m) to O(n×log(m)+n×k)
     * where k is the average number of transactions in the date window.
     * 
     * @param bankTransactions List of bank transactions to reconcile
     * @param ynabTransactions List of YNAB transactions to match against
     * @param matcher Strategy for determining transaction matches
     * @return Result containing matched and unmatched transactions
     */
    public TransactionMatchResult reconcileTransactions(
        List<BankTransaction> bankTransactions,
        List<YnabTransaction> ynabTransactions,
        TransactionMatcher matcher
    ) {
        Objects.requireNonNull(bankTransactions, "Bank transactions cannot be null");
        Objects.requireNonNull(ynabTransactions, "YNAB transactions cannot be null");
        Objects.requireNonNull(matcher, "Transaction matcher cannot be null");

        if (bankTransactions.isEmpty()) {
            return new TransactionMatchResult(List.of(), List.of());
        }

        if (ynabTransactions.isEmpty()) {
            return new TransactionMatchResult(List.of(), new ArrayList<>(bankTransactions));
        }

        return performOptimizedMatching(bankTransactions, ynabTransactions, matcher);
    }

    private TransactionMatchResult performOptimizedMatching(
        List<BankTransaction> bankTransactions,
        List<YnabTransaction> ynabTransactions,
        TransactionMatcher matcher
    ) {
        // Sort both transaction lists by date for optimal performance
        List<BankTransaction> sortedBankTransactions = bankTransactions.stream()
            .sorted(Comparator.comparing(BankTransaction::date))
            .toList();
            
        List<ReconcilableTransaction> sortedYnabTransactions = ynabTransactions.stream()
            .sorted(Comparator.comparing(YnabTransaction::date))
            .<ReconcilableTransaction>map(YnabTransactionAdapter::new)
            .toList();

        List<BankTransaction> matchedTransactions = new ArrayList<>();
        List<BankTransaction> missingFromYnab = new ArrayList<>();
        Set<Integer> matchedYnabIndices = new HashSet<>(); // Track which YNAB transactions have been matched

        // Process bank transactions in chronological order for better cache locality
        for (BankTransaction bankTransaction : sortedBankTransactions) {
            Integer matchedYnabIndex = findMatchForBankTransaction(
                bankTransaction, sortedYnabTransactions, matcher, matchedYnabIndices
            );

            if (matchedYnabIndex != null) {
                matchedTransactions.add(bankTransaction);
                matchedYnabIndices.add(matchedYnabIndex); // Mark this YNAB transaction as matched
            } else {
                missingFromYnab.add(bankTransaction);
            }
        }

        return new TransactionMatchResult(matchedTransactions, missingFromYnab);
    }

    private Integer findMatchForBankTransaction(
        BankTransaction bankTransaction,
        List<ReconcilableTransaction> sortedYnabTransactions,
        TransactionMatcher matcher,
        Set<Integer> matchedYnabIndices
    ) {
        ReconcilableTransaction reconcilableBankTransaction = new BankTransactionAdapter(bankTransaction);
        
        // Get the date window for this transaction based on the matcher strategy
        DateRange searchWindow = getSearchWindow(bankTransaction.date(), matcher.getStrategy());
        
        // Find the range of YNAB transactions within the date window
        int startIndex = findFirstTransactionInRange(sortedYnabTransactions, searchWindow.start());
        if (startIndex == -1) {
            return null; // No transactions in date range
        }

        // Check only transactions within the date window that haven't been matched yet
        for (int i = startIndex; i < sortedYnabTransactions.size(); i++) {
            ReconcilableTransaction ynabTransaction = sortedYnabTransactions.get(i);
            
            // Stop if we've moved beyond the search window
            if (ynabTransaction.date().isAfter(searchWindow.end())) {
                break;
            }
            
            // Skip if this YNAB transaction was already matched
            if (matchedYnabIndices.contains(i)) {
                continue;
            }
            
            if (matcher.matches(reconcilableBankTransaction, ynabTransaction)) {
                return i; // Return the index of the matched YNAB transaction
            }
        }

        return null; // No match found
    }

    private DateRange getSearchWindow(LocalDate transactionDate, ReconciliationStrategy strategy) {
        return switch (strategy) {
            case STRICT -> DateRange.ofSingleDay(transactionDate);
            case RANGE -> DateRange.ofDaysAround(transactionDate, 3, 3);
        };
    }

    private int findFirstTransactionInRange(List<ReconcilableTransaction> sortedTransactions, LocalDate startDate) {
        // Binary search for the first transaction >= startDate
        int left = 0;
        int right = sortedTransactions.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            LocalDate midDate = sortedTransactions.get(mid).date();

            if (!midDate.isBefore(startDate)) {
                result = mid;
                right = mid - 1; // Look for earlier occurrence
            } else {
                left = mid + 1;
            }
        }

        return result;
    }
}