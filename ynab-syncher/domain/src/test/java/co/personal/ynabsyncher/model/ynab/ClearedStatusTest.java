package co.personal.ynabsyncher.model.ynab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClearedStatus")
class ClearedStatusTest {

    @Test
    @DisplayName("should have correct enum values")
    void shouldHaveCorrectEnumValues() {
        assertThat(ClearedStatus.values()).hasSize(3);
        assertThat(ClearedStatus.valueOf("CLEARED")).isEqualTo(ClearedStatus.CLEARED);
        assertThat(ClearedStatus.valueOf("UNCLEARED")).isEqualTo(ClearedStatus.UNCLEARED);
        assertThat(ClearedStatus.valueOf("RECONCILED")).isEqualTo(ClearedStatus.RECONCILED);
    }
    
    @Test
    @DisplayName("should have stable ordinal values")
    void shouldHaveStableOrdinalValues() {
        assertThat(ClearedStatus.CLEARED.ordinal()).isEqualTo(0);
        assertThat(ClearedStatus.UNCLEARED.ordinal()).isEqualTo(1);
        assertThat(ClearedStatus.RECONCILED.ordinal()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        assertThat(ClearedStatus.CLEARED.toString()).isEqualTo("CLEARED");
        assertThat(ClearedStatus.UNCLEARED.toString()).isEqualTo("UNCLEARED");
        assertThat(ClearedStatus.RECONCILED.toString()).isEqualTo("RECONCILED");
    }
}
