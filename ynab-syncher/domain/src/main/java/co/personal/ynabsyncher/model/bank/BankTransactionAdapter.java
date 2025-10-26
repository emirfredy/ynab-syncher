package co.personal.ynabsyncher.model.bank;

import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.TransactionSource;
import co.personal.ynabsyncher.model.reconciliation.ReconcilableTransaction;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adapter that makes BankTransaction reconcilable.
 */
public record BankTransactionAdapter(BankTransaction transaction) implements ReconcilableTransaction {
    
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
        return transaction.inferredCategory();
    }
    
    @Override
    public TransactionSource source() {
        return TransactionSource.BANK;
    }
    
    @Override
    public String reconciliationContext() {
        String context = Optional.ofNullable(transaction.memo())
                .map(memo -> transaction.description() + " | " + memo)
                .orElse(transaction.description());
        
        if (transaction.hasCategoryInferred()) {
            context += " | " + transaction.inferredCategory().name();
        }
        
        return context;
    }
}