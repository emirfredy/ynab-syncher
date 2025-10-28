package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Budget DTO for JSON serialization/deserialization.
 */
public class YnabBudgetDto {
    private String id;
    private String name;
    private String lastModifiedOn;
    private String firstMonth;
    private String lastMonth;
    private String currencyFormat;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastModifiedOn() { return lastModifiedOn; }
    public void setLastModifiedOn(String lastModifiedOn) { this.lastModifiedOn = lastModifiedOn; }

    public String getFirstMonth() { return firstMonth; }
    public void setFirstMonth(String firstMonth) { this.firstMonth = firstMonth; }

    public String getLastMonth() { return lastMonth; }
    public void setLastMonth(String lastMonth) { this.lastMonth = lastMonth; }

    public String getCurrencyFormat() { return currencyFormat; }
    public void setCurrencyFormat(String currencyFormat) { this.currencyFormat = currencyFormat; }
}