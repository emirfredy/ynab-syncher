package co.personal.ynabsyncher.infrastructure.persistence.entity;

/**
 * JPA enum mapping for CategoryType.
 * Maps domain CategoryType to database representation.
 */
public enum CategoryTypeEntity {
    YNAB_ASSIGNED,
    BANK_INFERRED,
    UNKNOWN;
    
    public static CategoryTypeEntity fromDomainType(co.personal.ynabsyncher.model.CategoryType domainType) {
        return switch (domainType) {
            case YNAB_ASSIGNED -> YNAB_ASSIGNED;
            case BANK_INFERRED -> BANK_INFERRED;
            case UNKNOWN -> UNKNOWN;
        };
    }
    
    public co.personal.ynabsyncher.model.CategoryType toDomainType() {
        return switch (this) {
            case YNAB_ASSIGNED -> co.personal.ynabsyncher.model.CategoryType.YNAB_ASSIGNED;
            case BANK_INFERRED -> co.personal.ynabsyncher.model.CategoryType.BANK_INFERRED;
            case UNKNOWN -> co.personal.ynabsyncher.model.CategoryType.UNKNOWN;
        };
    }
}