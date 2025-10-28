package co.personal.ynabsyncher.infrastructure.client.dto;

import java.util.List;

/**
 * YNAB API Category Group DTO.
 */
public class YnabCategoryGroupDto {
    private String id;
    private String name;
    private List<YnabCategoryDto> categories;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<YnabCategoryDto> getCategories() { return categories; }
    public void setCategories(List<YnabCategoryDto> categories) { this.categories = categories; }
}