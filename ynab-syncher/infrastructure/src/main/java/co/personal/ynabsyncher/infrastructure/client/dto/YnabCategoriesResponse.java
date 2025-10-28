package co.personal.ynabsyncher.infrastructure.client.dto;

import java.util.List;

/**
 * YNAB API Categories response wrapper.
 */
public class YnabCategoriesResponse {
    private YnabCategoriesData data;
    
    public YnabCategoriesData getData() { return data; }
    public void setData(YnabCategoriesData data) { this.data = data; }
    
    public static class YnabCategoriesData {
        private List<YnabCategoryGroupDto> categoryGroups;
        
        public List<YnabCategoryGroupDto> getCategoryGroups() { return categoryGroups; }
        public void setCategoryGroups(List<YnabCategoryGroupDto> categoryGroups) { this.categoryGroups = categoryGroups; }
    }
}