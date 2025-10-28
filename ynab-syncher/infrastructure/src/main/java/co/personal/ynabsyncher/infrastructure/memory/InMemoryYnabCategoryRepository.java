package co.personal.ynabsyncher.infrastructure.memory;

import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.spi.repository.YnabCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory implementation of YnabCategoryRepository.
 * This is a placeholder implementation that provides some sample categories
 * for demonstration and testing purposes. In a real application, this would
 * connect to the YNAB API to fetch actual categories.
 */
@Repository
public class InMemoryYnabCategoryRepository implements YnabCategoryRepository {

    private final List<YnabCategory> categories;

    public InMemoryYnabCategoryRepository() {
        this.categories = initializeSampleCategories();
    }

    @Override
    public List<YnabCategory> findAllAvailableCategories() {
        return categories.stream()
                .filter(YnabCategory::isAvailableForInference)
                .toList();
    }

    /**
     * Initializes sample categories for demonstration purposes.
     */
    private List<YnabCategory> initializeSampleCategories() {
        List<YnabCategory> sampleCategories = new ArrayList<>();

        // Food & Dining categories
        sampleCategories.add(new YnabCategory("cat_1", "Groceries", "grp_1", "Food & Dining", false, false));
        sampleCategories.add(new YnabCategory("cat_2", "Restaurants", "grp_1", "Food & Dining", false, false));
        sampleCategories.add(new YnabCategory("cat_3", "Coffee Shops", "grp_1", "Food & Dining", false, false));
        sampleCategories.add(new YnabCategory("cat_4", "Fast Food", "grp_1", "Food & Dining", false, false));

        // Transportation categories
        sampleCategories.add(new YnabCategory("cat_5", "Gas & Fuel", "grp_2", "Transportation", false, false));
        sampleCategories.add(new YnabCategory("cat_6", "Auto Insurance", "grp_2", "Transportation", false, false));
        sampleCategories.add(new YnabCategory("cat_7", "Public Transportation", "grp_2", "Transportation", false, false));
        sampleCategories.add(new YnabCategory("cat_8", "Parking", "grp_2", "Transportation", false, false));

        // Shopping categories
        sampleCategories.add(new YnabCategory("cat_9", "Clothing", "grp_3", "Shopping", false, false));
        sampleCategories.add(new YnabCategory("cat_10", "Electronics", "grp_3", "Shopping", false, false));
        sampleCategories.add(new YnabCategory("cat_11", "General Merchandise", "grp_3", "Shopping", false, false));

        // Bills & Utilities categories
        sampleCategories.add(new YnabCategory("cat_12", "Electric", "grp_4", "Bills & Utilities", false, false));
        sampleCategories.add(new YnabCategory("cat_13", "Internet", "grp_4", "Bills & Utilities", false, false));
        sampleCategories.add(new YnabCategory("cat_14", "Phone", "grp_4", "Bills & Utilities", false, false));
        sampleCategories.add(new YnabCategory("cat_15", "Water", "grp_4", "Bills & Utilities", false, false));

        // Entertainment categories
        sampleCategories.add(new YnabCategory("cat_16", "Movies", "grp_5", "Entertainment", false, false));
        sampleCategories.add(new YnabCategory("cat_17", "Games", "grp_5", "Entertainment", false, false));
        sampleCategories.add(new YnabCategory("cat_18", "Streaming Services", "grp_5", "Entertainment", false, false));

        // Income categories
        sampleCategories.add(new YnabCategory("cat_19", "Salary", "grp_6", "Income", false, false));
        sampleCategories.add(new YnabCategory("cat_20", "Freelance", "grp_6", "Income", false, false));
        sampleCategories.add(new YnabCategory("cat_21", "Interest", "grp_6", "Income", false, false));

        // Add a hidden category to test filtering
        sampleCategories.add(new YnabCategory("cat_22", "Old Category", "grp_7", "Archived", true, false));

        return sampleCategories;
    }
}