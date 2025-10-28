package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Category DTO for JSON serialization/deserialization.
 */
public class YnabCategoryDto {
    private String id;
    private String name;
    private String categoryGroupId;
    private boolean hidden;
    private boolean deleted;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryGroupId() { return categoryGroupId; }
    public void setCategoryGroupId(String categoryGroupId) { this.categoryGroupId = categoryGroupId; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}