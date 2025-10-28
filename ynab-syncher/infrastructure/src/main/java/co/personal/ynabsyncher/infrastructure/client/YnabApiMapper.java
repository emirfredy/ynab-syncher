package co.personal.ynabsyncher.infrastructure.client;

import co.personal.ynabsyncher.infrastructure.client.dto.*;
import co.personal.ynabsyncher.model.AccountId;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryType;
import co.personal.ynabsyncher.model.Money;
import co.personal.ynabsyncher.model.TransactionId;
import co.personal.ynabsyncher.model.ynab.ClearedStatus;
import co.personal.ynabsyncher.model.ynab.YnabAccount;
import co.personal.ynabsyncher.model.ynab.YnabBudget;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maps between YNAB API DTOs and domain models.
 * Handles data format conversion including milliunits and date formats.
 */
@Component
public class YnabApiMapper {

    /**
     * Converts YNAB's milliunits (1000 = $1.00) to Money domain object.
     */
    public Money fromMilliunits(long milliunits) {
        return Money.ofMilliunits(milliunits);
    }

    /**
     * Converts Money domain object to YNAB's milliunits format.
     */
    public long toMilliunits(Money money) {
        return money.milliunits();
    }

    /**
     * Maps YNAB budget DTO to domain model.
     */
    public YnabBudget toDomain(YnabBudgetDto dto) {
        return new YnabBudget(
                dto.getId(),
                dto.getName(),
                OffsetDateTime.parse(dto.getLastModifiedOn()),
                dto.getFirstMonth() != null ? OffsetDateTime.parse(dto.getFirstMonth()) : null,
                dto.getLastMonth() != null ? OffsetDateTime.parse(dto.getLastMonth()) : null,
                dto.getCurrencyFormat()
        );
    }

    /**
     * Maps YNAB account DTO to domain model.
     */
    public YnabAccount toDomain(YnabAccountDto dto) {
        return new YnabAccount(
                new AccountId(dto.getId()),
                dto.getName(),
                mapAccountType(dto.getType()),
                dto.isOnBudget(),
                dto.isClosed(),
                dto.getNote(),
                fromMilliunits(dto.getBalance()),
                fromMilliunits(dto.getClearedBalance()),
                fromMilliunits(dto.getUnclearedBalance()),
                dto.getTransferPayeeId(),
                dto.isDirectImportLinked(),
                dto.getDirectImportInError(),
                dto.getLastReconciledAt() != null ? OffsetDateTime.parse(dto.getLastReconciledAt()) : null
        );
    }

    /**
     * Maps YNAB category DTO to domain model.
     */
    public YnabCategory toDomain(YnabCategoryDto dto, String groupName) {
        return new YnabCategory(
                dto.getId(),
                dto.getName(),
                dto.getCategoryGroupId(),
                groupName,
                dto.isHidden(),
                dto.isDeleted()
        );
    }

    /**
     * Maps YNAB transaction DTO to domain model.
     */
    public YnabTransaction toDomain(YnabTransactionDto dto) {
        return new YnabTransaction(
                new TransactionId(dto.getId()),
                new AccountId(dto.getAccountId()),
                LocalDate.parse(dto.getDate()),
                fromMilliunits(dto.getAmount()),
                dto.getPayeeName(),
                dto.getMemo(),
                new Category(dto.getCategoryId(), dto.getCategoryName(), CategoryType.YNAB_ASSIGNED),
                mapClearedStatus(dto.getCleared()),
                dto.isApproved(),
                dto.getFlagColor()
        );
    }

    /**
     * Maps domain transaction to YNAB API DTO for creation/update.
     */
    public YnabTransactionDto toApi(YnabTransaction transaction) {
        YnabTransactionDto dto = new YnabTransactionDto();
        dto.setAccountId(transaction.accountId().value());
        dto.setDate(transaction.date().format(DateTimeFormatter.ISO_LOCAL_DATE));
        dto.setAmount(toMilliunits(transaction.amount()));
        dto.setPayeeName(transaction.payeeName());
        dto.setMemo(transaction.memo());
        dto.setCategoryId(transaction.category().id());
        dto.setCleared(mapClearedStatus(transaction.clearedStatus()));
        dto.setApproved(transaction.approved());
        dto.setFlagColor(transaction.flagColor());
        return dto;
    }

    private YnabAccount.AccountType mapAccountType(String apiType) {
        return switch (apiType.toUpperCase()) {
            case "CHECKING" -> YnabAccount.AccountType.CHECKING;
            case "SAVINGS" -> YnabAccount.AccountType.SAVINGS;
            case "CASH" -> YnabAccount.AccountType.CASH;
            case "CREDITCARD" -> YnabAccount.AccountType.CREDIT_CARD;
            case "LINEOFCREDIT" -> YnabAccount.AccountType.LINE_OF_CREDIT;
            case "OTHERASSET" -> YnabAccount.AccountType.OTHER_ASSET;
            case "OTHERLIABILITY" -> YnabAccount.AccountType.OTHER_LIABILITY;
            case "MORTGAGE" -> YnabAccount.AccountType.MORTGAGE;
            case "AUTOLOAN" -> YnabAccount.AccountType.AUTO_LOAN;
            case "STUDENTLOAN" -> YnabAccount.AccountType.STUDENT_LOAN;
            case "PERSONALLOAN" -> YnabAccount.AccountType.PERSONAL_LOAN;
            case "MEDICALDEBT" -> YnabAccount.AccountType.MEDICAL_DEBT;
            case "OTHERDEBT" -> YnabAccount.AccountType.OTHER_DEBT;
            default -> YnabAccount.AccountType.OTHER_ASSET;
        };
    }

    private ClearedStatus mapClearedStatus(String apiStatus) {
        return switch (apiStatus.toLowerCase()) {
            case "cleared" -> ClearedStatus.CLEARED;
            case "uncleared" -> ClearedStatus.UNCLEARED;
            case "reconciled" -> ClearedStatus.RECONCILED;
            default -> ClearedStatus.UNCLEARED;
        };
    }

    private String mapClearedStatus(ClearedStatus status) {
        return switch (status) {
            case CLEARED -> "cleared";
            case UNCLEARED -> "uncleared";
            case RECONCILED -> "reconciled";
        };
    }
}