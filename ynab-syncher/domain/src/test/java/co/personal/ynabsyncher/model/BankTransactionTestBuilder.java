package co.personal.ynabsyncher.model;

import co.personal.ynabsyncher.model.bank.BankTransaction;

import java.time.LocalDate;

/**
 * Test builder for creating BankTransaction instances in tests.
 */
public final class BankTransactionTestBuilder {
    private TransactionId id = TransactionId.of("bank-1");
    private AccountId accountId = AccountId.of("test-account");
    private LocalDate date = LocalDate.of(2024, 1, 15);
    private Money amount = Money.of(100.00);
    private String description = "Test Description";
    private String merchantName = "Test Merchant";
    private String memo = "Test Memo";
    private String transactionType = "DEBIT";
    private String reference = "REF123";
    private Category inferredCategory = Category.unknown();

    private BankTransactionTestBuilder() {}

    public static BankTransactionTestBuilder aBankTransaction() {
        return new BankTransactionTestBuilder();
    }

    public BankTransactionTestBuilder withId(String id) {
        this.id = TransactionId.of(id);
        return this;
    }

    public BankTransactionTestBuilder withId(TransactionId id) {
        this.id = id;
        return this;
    }

    public BankTransactionTestBuilder withAccountId(String accountId) {
        this.accountId = AccountId.of(accountId);
        return this;
    }

    public BankTransactionTestBuilder withAccountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    public BankTransactionTestBuilder withDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public BankTransactionTestBuilder withAmount(double amount) {
        this.amount = Money.of(amount);
        return this;
    }

    public BankTransactionTestBuilder withAmount(Money amount) {
        this.amount = amount;
        return this;
    }

    public BankTransactionTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public BankTransactionTestBuilder withMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public BankTransactionTestBuilder withMemo(String memo) {
        this.memo = memo;
        return this;
    }

    public BankTransactionTestBuilder withTransactionType(String transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public BankTransactionTestBuilder withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public BankTransactionTestBuilder withInferredCategory(Category category) {
        this.inferredCategory = category;
        return this;
    }

    public BankTransaction build() {
        return new BankTransaction(id, accountId, date, amount, description, 
                                 merchantName, memo, transactionType, reference, inferredCategory);
    }
}