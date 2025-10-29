package co.personal.ynabsyncher.usecase;

import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsRequest;
import co.personal.ynabsyncher.api.dto.SaveCategoryMappingsResponse;
import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.CategoryMappingId;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SaveCategoryMappingsUseCase")
class SaveCategoryMappingsUseCaseTest {

    @Mock
    private CategoryMappingRepository categoryMappingRepository;

    private SaveCategoryMappingsUseCase useCase;
    
    // Test data
    private Category groceryCategory;
    private Category gasCategory;
    private CategoryMapping groceryMapping;
    private CategoryMapping gasMapping;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new SaveCategoryMappingsUseCase(categoryMappingRepository);
        
        // Set up test data
        groceryCategory = Category.ynabCategory("grocery-id", "Groceries");
        gasCategory = Category.ynabCategory("gas-id", "Gas & Fuel");
        
        groceryMapping = new CategoryMapping(
            CategoryMappingId.generate(),
            groceryCategory,
            Set.of("SAFEWAY", "WHOLE FOODS"),
            0.9,
            3
        );
        
        gasMapping = new CategoryMapping(
            CategoryMappingId.generate(),
            gasCategory,
            Set.of("SHELL", "CHEVRON"),
            0.85,
            2
        );
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw exception when repository is null")
        void shouldThrowExceptionWhenRepositoryIsNull() {
            assertThatThrownBy(() -> new SaveCategoryMappingsUseCase(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Category mapping repository cannot be null");
        }
    }

    @Nested
    @DisplayName("Request validation")
    class RequestValidation {

        @Test
        @DisplayName("should throw exception when request is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> useCase.saveCategoryMappings(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request cannot be null");
        }
    }

    @Nested
    @DisplayName("Successful scenarios")
    class SuccessfulScenarios {

        @Test
        @DisplayName("should save new mappings when no conflicts exist")
        void shouldSaveNewMappingsWhenNoConflictsExist() {
            // Given
            when(categoryMappingRepository.findMappingsForPattern(any(TransactionPattern.class)))
                .thenReturn(List.of()); // No existing mappings
            when(categoryMappingRepository.save(any(CategoryMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(groceryMapping, gasMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.isCompleteSuccess()).isTrue();
            assertThat(response.totalRequested()).isEqualTo(2);
            assertThat(response.savedNew()).isEqualTo(2);
            assertThat(response.updatedExisting()).isEqualTo(0);
            assertThat(response.skipped()).isEqualTo(0);
            assertThat(response.savedMappings()).hasSize(2);
            assertThat(response.warnings()).isEmpty();
            assertThat(response.errors()).isEmpty();

            verify(categoryMappingRepository).save(groceryMapping);
            verify(categoryMappingRepository).save(gasMapping);
        }

        @Test
        @DisplayName("should consolidate mappings for same category")
        void shouldConsolidateMappingsForSameCategory() {
            // Given
            CategoryMapping existingGroceryMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory,
                Set.of("TRADER JOES"),
                0.8,
                2
            );

            CategoryMapping newGroceryMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory,
                Set.of("SAFEWAY"),
                0.9,
                1
            );

            when(categoryMappingRepository.findMappingsForPattern(any(TransactionPattern.class)))
                .thenReturn(List.of(existingGroceryMapping));
            when(categoryMappingRepository.save(any(CategoryMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(newGroceryMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.isCompleteSuccess()).isTrue();
            assertThat(response.totalRequested()).isEqualTo(1);
            assertThat(response.savedNew()).isEqualTo(0);
            assertThat(response.updatedExisting()).isEqualTo(1);
            assertThat(response.skipped()).isEqualTo(0);

            // Verify the consolidated mapping has both patterns
            CategoryMapping savedMapping = response.savedMappings().get(0);
            assertThat(savedMapping.textPatterns()).containsAll(Set.of("TRADER JOES", "SAFEWAY"));
            assertThat(savedMapping.occurrenceCount()).isEqualTo(3); // Incremented
        }
    }

    @Nested
    @DisplayName("Conflict handling")
    class ConflictHandling {

        @Test
        @DisplayName("should skip when conflicting categorization exists with higher confidence")
        void shouldSkipWhenConflictingCategorizationExistsWithHigherConfidence() {
            // Given
            CategoryMapping existingMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                gasCategory,
                Set.of("AMAZON"), // Same pattern
                0.95, // Higher confidence
                5
            );

            CategoryMapping conflictingMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory, // Different category
                Set.of("AMAZON"), // Same pattern
                0.7, // Lower confidence
                1
            );

            when(categoryMappingRepository.findMappingsForPattern(any(TransactionPattern.class)))
                .thenReturn(List.of(existingMapping));

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(conflictingMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.hasWarnings()).isTrue();
            assertThat(response.isCompleteSuccess()).isFalse();
            assertThat(response.totalRequested()).isEqualTo(1);
            assertThat(response.savedNew()).isEqualTo(0);
            assertThat(response.updatedExisting()).isEqualTo(0);
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(response.warnings()).hasSize(1);
            assertThat(response.warnings().get(0)).contains("higher confidence");
        }

        @Test
        @DisplayName("should skip when significant pattern overlap detected")
        void shouldSkipWhenSignificantPatternOverlapDetected() {
            // Given
            CategoryMapping existingMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                gasCategory,
                Set.of("AMAZON", "AMAZON FRESH"),
                0.8,
                3
            );

            CategoryMapping overlappingMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory, // Different category
                Set.of("AMAZON", "AMAZON PRIME"), // 50% overlap
                0.8, // Similar confidence
                1
            );

            when(categoryMappingRepository.findMappingsForPattern(any(TransactionPattern.class)))
                .thenReturn(List.of(existingMapping));

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(overlappingMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.hasWarnings()).isTrue();
            assertThat(response.isCompleteSuccess()).isFalse();
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(response.warnings().get(0)).contains("Conflicting categorization detected");
        }
    }

    @Nested
    @DisplayName("Quality validation")
    class QualityValidation {

        @Test
        @DisplayName("should skip mappings with too low confidence")
        void shouldSkipMappingsWithTooLowConfidence() {
            // Given
            CategoryMapping lowConfidenceMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory,
                Set.of("SOME STORE"),
                0.05, // Too low confidence
                1
            );

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(lowConfidenceMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.hasWarnings()).isTrue();
            assertThat(response.isCompleteSuccess()).isFalse();
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(response.warnings().get(0)).contains("Mapping quality too low");
        }

        @Test
        @DisplayName("should skip mappings with generic patterns")
        void shouldSkipMappingsWithGenericPatterns() {
            // Given
            CategoryMapping genericMapping = new CategoryMapping(
                CategoryMappingId.generate(),
                groceryCategory,
                Set.of("POS", "ATM"), // Generic patterns
                0.8,
                1
            );

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(genericMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.hasWarnings()).isTrue();
            assertThat(response.isCompleteSuccess()).isFalse();
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(response.warnings().get(0)).contains("Mapping quality too low");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should return failure response when repository throws exception")
        void shouldReturnFailureResponseWhenRepositoryThrowsException() {
            // Given
            when(categoryMappingRepository.findMappingsForPattern(any(TransactionPattern.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            SaveCategoryMappingsRequest request = new SaveCategoryMappingsRequest(
                List.of(groceryMapping)
            );

            // When
            SaveCategoryMappingsResponse response = useCase.saveCategoryMappings(request);

            // Then
            assertThat(response.hasErrors()).isTrue();
            assertThat(response.totalRequested()).isEqualTo(1);
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0)).contains("Database connection failed");
        }
    }
}