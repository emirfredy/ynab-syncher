package co.personal.ynabsyncher.spi.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for BankTransactionRepository interface.
 * Validates SPI boundary constraints and method patterns.
 * 
 * These tests enforce architectural decisions and catch interface mismatches
 * that could lead to compilation failures in infrastructure implementations.
 * 
 * The repository is read-only and supports querying existing bank data.
 * Transaction importing is handled in-memory without persistence.
 */
@DisplayName("BankTransactionRepository Contract")
class BankTransactionRepositoryContractTest {

    @Test
    @DisplayName("should only have read operations (no write operations)")
    void shouldOnlyHaveReadOperations() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> readMethodNames = Arrays.stream(methods)
            .map(Method::getName)
            .filter(name -> name.startsWith("find") || 
                           name.startsWith("get") || 
                           name.startsWith("count") || 
                           name.startsWith("exists"))
            .toList();

        List<String> writeMethodNames = Arrays.stream(methods)
            .map(Method::getName)
            .filter(name -> name.startsWith("save") || 
                           name.startsWith("delete") || 
                           name.startsWith("update") || 
                           name.startsWith("insert") ||
                           name.startsWith("create"))
            .toList();
        
        assertThat(readMethodNames)
            .as("Repository should have read operations for querying existing data")
            .isNotEmpty();
            
        assertThat(writeMethodNames)
            .as("Repository should be read-only (no write operations for in-memory processing)")
            .isEmpty();
    }

    @Test
    @DisplayName("should only have read operations and no write operations")
    void shouldOnlyHaveReadOperationsAndNoWriteOperations() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> methodNames = Arrays.stream(methods)
            .map(Method::getName)
            .toList();
        
        assertThat(methodNames)
            .as("All methods should be queries only (read-only repository)")
            .allMatch(name -> name.startsWith("find") || 
                             name.startsWith("get") || 
                             name.startsWith("count") || 
                             name.startsWith("exists"));
    }

    @Test
    @DisplayName("should have required query methods for current use cases")
    void shouldHaveRequiredQueryMethodsForCurrentUseCases() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> methodNames = Arrays.stream(methods)
            .map(Method::getName)
            .toList();
        
        // Validate required methods exist for current use cases
        assertThat(methodNames)
            .as("Required methods for current use cases")
            .contains("findByIds", "findByAccountIdAndDateRange");
    }

    @Test
    @DisplayName("should not expose infrastructure concerns in method signatures")
    void shouldNotExposeInfrastructureConcerns() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        for (Method method : methods) {
            // Check return types don't expose infrastructure
            String returnType = method.getReturnType().getName();
            assertThat(returnType)
                .as("Method %s should not return infrastructure types", method.getName())
                .doesNotContain("org.springframework", "jakarta.persistence", "org.hibernate");
            
            // Check parameters don't expose infrastructure
            for (Class<?> paramType : method.getParameterTypes()) {
                assertThat(paramType.getName())
                    .as("Method %s should not accept infrastructure types", method.getName())
                    .doesNotContain("org.springframework", "jakarta.persistence", "org.hibernate");
            }
        }
    }
}