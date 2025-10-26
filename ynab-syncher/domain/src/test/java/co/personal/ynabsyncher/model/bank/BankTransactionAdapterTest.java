package co.personal.ynabsyncher.model.bank;

import co.personal.ynabsyncher.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static co.personal.ynabsyncher.model.BankTransactionTestBuilder.aBankTransaction;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BankTransactionAdapter")
class BankTransactionAdapterTest {

    @Test
    @DisplayName("should adapt bank transaction to reconcilable transaction")
    void shouldAdaptBankTransactionToReconcilableTransaction() {
        var bankTransaction = aBankTransaction()
            .withId("bank-1")
            .withAccountId("acc-1")
            .withDate(LocalDate.of(2024, 1, 15))
            .withAmount(100.50)
            .withDescription("Coffee Shop")
            .withMerchantName("Starbucks")
            .withMemo("Morning coffee")
            .withInferredCategory(Category.inferredCategory("Dining"))
            .build();
        
        var adapter = new BankTransactionAdapter(bankTransaction);
        
        assertThat(adapter.id()).isEqualTo(TransactionId.of("bank-1"));
        assertThat(adapter.accountId()).isEqualTo(AccountId.of("acc-1"));
        assertThat(adapter.date()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(adapter.amount()).isEqualTo(Money.of(100.50));
        assertThat(adapter.displayName()).isEqualTo("Starbucks");
        assertThat(adapter.category()).isEqualTo(Category.inferredCategory("Dining"));
        assertThat(adapter.source()).isEqualTo(TransactionSource.BANK);
    }
    
    @Test
    @DisplayName("should build reconciliation context with inferred category")
    void shouldBuildReconciliationContextWithInferredCategory() {
        var bankTransaction = aBankTransaction()
            .withDescription("PURCHASE AT STARBUCKS")
            .withMemo("Coffee")
            .withInferredCategory(Category.inferredCategory("Dining"))
            .build();
        
        var adapter = new BankTransactionAdapter(bankTransaction);
        
        var context = adapter.reconciliationContext();
        assertThat(context).contains("PURCHASE AT STARBUCKS");
        assertThat(context).contains("Coffee");
        assertThat(context).contains("Dining");
    }
    
    @Test
    @DisplayName("should build reconciliation context without memo")
    void shouldBuildReconciliationContextWithoutMemo() {
        var bankTransaction = aBankTransaction()
            .withDescription("ATM WITHDRAWAL")
            .withMemo(null)
            .withInferredCategory(Category.inferredCategory("Cash"))
            .build();
        
        var adapter = new BankTransactionAdapter(bankTransaction);
        
        var context = adapter.reconciliationContext();
        assertThat(context).isEqualTo("ATM WITHDRAWAL | Cash");
    }
    
    @Test
    @DisplayName("should build reconciliation context without inferred category")
    void shouldBuildReconciliationContextWithoutInferredCategory() {
        var bankTransaction = aBankTransaction()
            .withDescription("UNKNOWN MERCHANT")
            .withMemo("Unknown transaction")
            .withInferredCategory(Category.unknown())
            .build();
        
        var adapter = new BankTransactionAdapter(bankTransaction);
        
        var context = adapter.reconciliationContext();
        assertThat(context).isEqualTo("UNKNOWN MERCHANT | Unknown transaction");
    }
    
    @Test
    @DisplayName("should expose wrapped transaction")
    void shouldExposeWrappedTransaction() {
        var bankTransaction = aBankTransaction().build();
        var adapter = new BankTransactionAdapter(bankTransaction);
        
        assertThat(adapter.transaction()).isSameAs(bankTransaction);
    }
}
