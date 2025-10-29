# CreateMissingTransactions Use Case - Usage Example

## Overview

The `CreateMissingTransactions` use case has been implemented with a reference-based approach that eliminates data duplication and provides a clean API for handling both successful and failed transaction creations.

## Key Features

- **Reference-based results**: `TransactionCreationResult` maintains a reference to the original `BankTransaction` instead of duplicating data
- **Type-safe success/failure handling**: Uses `Optional<TransactionId>` for success and `Optional<String>` for error messages
- **Convenient filtering methods**: Easy access to successful and failed results
- **Memory efficient**: No data duplication between results and original transactions

## Usage Example

```java
// 1. Load bank transactions from CSV (or other source)
List<BankTransaction> bankTransactions = csvLoader.loadTransactions("transactions.csv");

// 2. Reconcile to find missing transactions
ReconciliationResult reconciliation = reconcileTransactions.reconcile(reconciliationRequest);
List<BankTransaction> missingFromYnab = reconciliation.missingFromYnab();

// 3. Create missing transactions in YNAB
CreateMissingTransactionsRequest request = new CreateMissingTransactionsRequest(
    budgetId,
    bankAccountId,
    ynabAccountId,
    missingFromYnab  // Direct use of bank transactions from reconciliation
);

CreateMissingTransactionsResponse response = createMissingTransactions.createMissingTransactions(request);

// 4. Handle results
if (response.allSuccessful()) {
    System.out.println("All " + response.successfullyCreated() + " transactions created successfully!");
} else {
    System.out.println("Created: " + response.successfullyCreated() +
                      ", Failed: " + response.failed());
}

// 5. Process successful transactions
List<TransactionCreationResult> successful = response.getSuccessfulResults();
for (TransactionCreationResult result : successful) {
    BankTransaction originalTxn = result.originalTransaction();
    TransactionId ynabId = result.ynabTransactionId().get();

    System.out.println("✓ Created YNAB transaction " + ynabId.value() +
                      " for bank transaction: " + originalTxn.description());
}

// 6. Handle failed transactions
List<TransactionCreationResult> failed = response.getFailedResults();
for (TransactionCreationResult result : failed) {
    BankTransaction originalTxn = result.originalTransaction();
    String error = result.errorMessage().get();

    System.err.println("✗ Failed to create transaction for: " + originalTxn.description() +
                      " - Error: " + error);

    // Could implement retry logic, manual review queue, etc.
}
```

## API Design Benefits

### 1. Single Source of Truth

```java
// ❌ Old approach - data duplication
TransactionCreationResult oldResult = TransactionCreationResult.success(
    ynabId, description, amount, date  // Duplicated from BankTransaction
);

// ✅ New approach - reference to original
TransactionCreationResult newResult = TransactionCreationResult.success(
    bankTransaction,  // Reference to single source
    ynabId
);
```

### 2. Type Safety

```java
// ✅ Compile-time guarantees
if (result.wasSuccessful()) {
    TransactionId ynabId = result.ynabTransactionId().get();  // Safe to call .get()
} else {
    String error = result.errorMessage().get();  // Safe to call .get()
}
```

### 3. Rich Context

```java
// Access full bank transaction context
BankTransaction original = result.originalTransaction();
String merchantName = original.merchantName();
Category inferredCategory = original.inferredCategory();
Money amount = original.amount();
// ... all original fields available
```

### 4. Easy Filtering

```java
// Built-in convenience methods
List<TransactionCreationResult> successful = response.getSuccessfulResults();
List<TransactionCreationResult> failed = response.getFailedResults();

// Or custom filtering
List<BankTransaction> failedGroceryTransactions = response.getFailedResults()
    .stream()
    .map(TransactionCreationResult::originalTransaction)
    .filter(txn -> txn.inferredCategory().name().contains("Grocery"))
    .toList();
```

## Memory Considerations

The reference-based approach keeps `BankTransaction` objects in memory longer, but this is acceptable because:

1. **CSV transactions are already in memory** for processing
2. **No data duplication** - more memory efficient overall
3. **Rich context preserved** - enables better error handling and reporting
4. **Short-lived objects** - released after response processing

This design aligns with the hexagonal architecture principles and provides a clean, type-safe API for handling transaction creation results.
