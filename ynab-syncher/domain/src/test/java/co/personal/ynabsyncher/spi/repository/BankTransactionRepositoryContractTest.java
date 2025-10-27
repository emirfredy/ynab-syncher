package co.personal.ynabsyncher.spi.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for BankTransactionRepository interface.
 * Validates SPI boundary constraints and read-only patterns.
 * 
 * These tests enforce architectural decisions and catch interface mismatches
 * that could lead to compilation failures in infrastructure implementations.
 */
@DisplayName("BankTransactionRepository Contract")
class BankTransactionRepositoryContractTest {

    @Test
    @DisplayName("should be read-only repository with no write operations")
    void shouldBeReadOnlyRepository() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> writeMethodNames = Arrays.stream(methods)
            .map(Method::getName)
            .filter(name -> name.startsWith("save") || 
                           name.startsWith("delete") || 
                           name.startsWith("update") || 
                           name.startsWith("insert") ||
                           name.startsWith("create"))
            .toList();
        
        assertThat(writeMethodNames)
            .as("BankTransactionRepository should not have write operations (read-only external data)")
            .isEmpty();
    }

    @Test
    @DisplayName("should only have query methods following naming conventions")
    void shouldOnlyHaveQueryMethods() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> methodNames = Arrays.stream(methods)
            .map(Method::getName)
            .toList();
        
        assertThat(methodNames)
            .as("All methods should be queries (find*, get*, count*, exists*)")
            .allMatch(name -> name.startsWith("find") || 
                             name.startsWith("get") || 
                             name.startsWith("count") || 
                             name.startsWith("exists"));
    }

    @Test
    @DisplayName("should have required query methods for current use cases")
    void shouldHaveRequiredQueryMethods() {
        Method[] methods = BankTransactionRepository.class.getDeclaredMethods();
        
        List<String> methodNames = Arrays.stream(methods)
            .map(Method::getName)
            .toList();
        
        // Validate required methods exist for current use cases
        assertThat(methodNames)
            .as("Required methods for current use cases")
            .contains("findByIds");
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