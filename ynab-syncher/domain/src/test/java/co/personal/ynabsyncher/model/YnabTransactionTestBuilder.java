package co.personal.ynabsyncher.model;

import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;

import java.time.LocalDate;

/**
 * Test builder for creating YnabTransaction instances in tests.
 */
public final class YnabTransactionTestBuilder {
    private TransactionId id = TransactionId.of("ynab-1");
    private AccountId accountId = AccountId.of("test-account");
    private LocalDate date = LocalDate.of(2024, 1, 15);
    private Money amount = Money.of(100.00);
    private String payeeName = "Test Payee";
    private String memo = "Test Memo";
    private Category category = Category.ynabCategory("cat1", "Test Category");
    private ClearedStatus clearedStatus = ClearedStatus.CLEARED;
    private boolean approved = true;
    private String flagColor = null;

    private YnabTransactionTestBuilder() {}

    public static YnabTransactionTestBuilder aYnabTransaction() {
        return new YnabTransactionTestBuilder();
    }

    public YnabTransactionTestBuilder withId(String id) {
        this.id = TransactionId.of(id);
        return this;
    }

    public YnabTransactionTestBuilder withId(TransactionId id) {
        this.id = id;
        return this;
    }

    public YnabTransactionTestBuilder withAccountId(String accountId) {
        this.accountId = AccountId.of(accountId);
        return this;
    }

    public YnabTransactionTestBuilder withAccountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    public YnabTransactionTestBuilder withDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public YnabTransactionTestBuilder withAmount(double amount) {
        this.amount = Money.of(amount);
        return this;
    }

    public YnabTransactionTestBuilder withAmount(Money amount) {
        this.amount = amount;
        return this;
    }

    public YnabTransactionTestBuilder withPayeeName(String payeeName) {
        this.payeeName = payeeName;
        return this;
    }

    public YnabTransactionTestBuilder withMemo(String memo) {
        this.memo = memo;
        return this;
    }

    public YnabTransactionTestBuilder withCategory(Category category) {
        this.category = category;
        return this;
    }

    public YnabTransactionTestBuilder withClearedStatus(ClearedStatus clearedStatus) {
        this.clearedStatus = clearedStatus;
        return this;
    }

    public YnabTransactionTestBuilder withApproved(boolean approved) {
        this.approved = approved;
        return this;
    }

    public YnabTransactionTestBuilder withFlagColor(String flagColor) {
        this.flagColor = flagColor;
        return this;
    }

    public YnabTransaction build() {
        return new YnabTransaction(id, accountId, date, amount, payeeName, 
                                 memo, category, clearedStatus, approved, flagColor);
    }
}