package co.personal.ynabsyncher.spi.repository;

import co.personal.ynabsyncher.model.ynab.YnabCategory;

import java.util.List;

/**
 * Repository interface for accessing YNAB categories.
 * This SPI port abstracts the data access for category inference without
 * coupling the domain to specific YNAB API implementations.
 */
public interface YnabCategoryRepository {
    
    /**
     * Retrieves all available YNAB categories for inference.
     * Returns only categories that are not hidden or deleted.
     * 
     * @return List of active YNAB categories suitable for inference
     */
    List<YnabCategory> findAllAvailableCategories();
}