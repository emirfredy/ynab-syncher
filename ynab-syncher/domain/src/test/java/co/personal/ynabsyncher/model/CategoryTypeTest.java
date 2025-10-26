package co.personal.ynabsyncher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryType")
class CategoryTypeTest {

    @Test
    @DisplayName("should have correct enum values")
    void shouldHaveCorrectEnumValues() {
        assertThat(CategoryType.values()).hasSize(3);
        assertThat(CategoryType.valueOf("YNAB_ASSIGNED")).isEqualTo(CategoryType.YNAB_ASSIGNED);
        assertThat(CategoryType.valueOf("BANK_INFERRED")).isEqualTo(CategoryType.BANK_INFERRED);
        assertThat(CategoryType.valueOf("UNKNOWN")).isEqualTo(CategoryType.UNKNOWN);
    }
    
    @Test
    @DisplayName("should have stable ordinal values")
    void shouldHaveStableOrdinalValues() {
        assertThat(CategoryType.YNAB_ASSIGNED.ordinal()).isEqualTo(0);
        assertThat(CategoryType.BANK_INFERRED.ordinal()).isEqualTo(1);
        assertThat(CategoryType.UNKNOWN.ordinal()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        assertThat(CategoryType.YNAB_ASSIGNED.toString()).isEqualTo("YNAB_ASSIGNED");
        assertThat(CategoryType.BANK_INFERRED.toString()).isEqualTo("BANK_INFERRED");
        assertThat(CategoryType.UNKNOWN.toString()).isEqualTo("UNKNOWN");
    }
}
