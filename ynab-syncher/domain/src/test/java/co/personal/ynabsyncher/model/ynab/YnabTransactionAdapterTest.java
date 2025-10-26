package co.personal.ynabsyncher.model.ynab;

import co.personal.ynabsyncher.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static co.personal.ynabsyncher.model.YnabTransactionTestBuilder.aYnabTransaction;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YnabTransactionAdapter")
class YnabTransactionAdapterTest {

    @Test
    @DisplayName("should adapt YNAB transaction to reconcilable transaction")
    void shouldAdaptYnabTransactionToReconcilableTransaction() {
        var ynabTransaction = aYnabTransaction()
            .withId("ynab-1")
            .withAccountId("acc-1")
            .withDate(LocalDate.of(2024, 1, 15))
            .withAmount(75.25)
            .withPayeeName("Grocery Store")
            .withCategory(Category.ynabCategory("cat1", "Groceries"))
            .withClearedStatus(ClearedStatus.CLEARED)
            .build();
        
        var adapter = new YnabTransactionAdapter(ynabTransaction);
        
        assertThat(adapter.id()).isEqualTo(TransactionId.of("ynab-1"));
        assertThat(adapter.accountId()).isEqualTo(AccountId.of("acc-1"));
        assertThat(adapter.date()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(adapter.amount()).isEqualTo(Money.of(75.25));
        assertThat(adapter.displayName()).isEqualTo("Grocery Store");
        assertThat(adapter.category()).isEqualTo(Category.ynabCategory("cat1", "Groceries"));
        assertThat(adapter.source()).isEqualTo(TransactionSource.YNAB);
    }
    
    @Test
    @DisplayName("should build reconciliation context with memo")
    void shouldBuildReconciliationContextWithMemo() {
        var ynabTransaction = aYnabTransaction()
            .withPayeeName("Amazon")
            .withMemo("Office supplies")
            .withCategory(Category.ynabCategory("cat1", "Business"))
            .build();
        
        var adapter = new YnabTransactionAdapter(ynabTransaction);
        
        var context = adapter.reconciliationContext();
        assertThat(context).isEqualTo("Amazon | Office supplies | Business");
    }
    
    @Test
    @DisplayName("should build reconciliation context without memo")
    void shouldBuildReconciliationContextWithoutMemo() {
        var ynabTransaction = aYnabTransaction()
            .withPayeeName("Gas Station")
            .withMemo(null)
            .withCategory(Category.ynabCategory("cat1", "Transportation"))
            .build();
        
        var adapter = new YnabTransactionAdapter(ynabTransaction);
        
        var context = adapter.reconciliationContext();
        assertThat(context).isEqualTo("Gas Station | Transportation");
    }
    
    @Test
    @DisplayName("should expose wrapped transaction")
    void shouldExposeWrappedTransaction() {
        var ynabTransaction = aYnabTransaction().build();
        var adapter = new YnabTransactionAdapter(ynabTransaction);
        
        assertThat(adapter.transaction()).isSameAs(ynabTransaction);
    }
}
