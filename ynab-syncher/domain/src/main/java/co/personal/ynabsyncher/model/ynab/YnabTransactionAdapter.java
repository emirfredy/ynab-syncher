package co.personal.ynabsyncher.model.ynab;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.TransactionSource;
import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adapter that makes YnabTransaction reconcilable.
 */
public record YnabTransactionAdapter(YnabTransaction transaction) implements ReconcilableTransaction {
    
    @Override
    public TransactionId id() {
        return transaction.id();
    }
    
    @Override
    public AccountId accountId() {
        return transaction.accountId();
    }
    
    @Override
    public LocalDate date() {
        return transaction.date();
    }
    
    @Override
    public Money amount() {
        return transaction.amount();
    }
    
    @Override
    public String displayName() {
        return transaction.displayName();
    }
    
    @Override
    public Category category() {
        return transaction.category();
    }
    
    @Override
    public TransactionSource source() {
        return TransactionSource.YNAB;
    }
    
    @Override
    public String reconciliationContext() {
        return Optional.ofNullable(transaction.memo())
                .map(memo -> transaction.payeeName() + " | " + memo + " | " + transaction.category().name())
                .orElse(transaction.payeeName() + " | " + transaction.category().name());
    }
}