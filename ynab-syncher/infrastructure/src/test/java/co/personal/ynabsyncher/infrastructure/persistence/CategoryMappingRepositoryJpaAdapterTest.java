package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.infrastructure.persistence.jpa.CategoryMappingJpaRepository;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * JPA-specific tests for CategoryMappingRepositoryJpaAdapter.
 * Extends the contract test to ensure behavioral compliance.
 */
@DataJpaTest
@Import(CategoryMappingRepositoryJpaAdapter.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false"
})
@DisplayName("CategoryMappingRepository JPA Implementation")
class CategoryMappingRepositoryJpaAdapterTest extends CategoryMappingRepositoryContractTest {

    @Autowired
    private CategoryMappingRepositoryJpaAdapter adapter;

    @Autowired
    private CategoryMappingJpaRepository jpaRepository;

    @Override
    protected CategoryMappingRepository repository() {
        return adapter;
    }

    @Override
    protected void clearRepository() {
        // Clear all data between tests
        jpaRepository.deleteAll();
        jpaRepository.flush(); // Ensure deletion is committed
    }
}