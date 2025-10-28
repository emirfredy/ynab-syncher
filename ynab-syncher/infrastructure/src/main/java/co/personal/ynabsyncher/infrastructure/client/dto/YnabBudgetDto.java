package co.personal.ynabsyncher.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YNAB API Budget DTO for JSON serialization/deserialization.
 */
public class YnabBudgetDto {
    private String id;
    private String name;
    @JsonProperty("last_modified_on")
    private String lastModifiedOn;
    @JsonProperty("first_month")
    private String firstMonth;
    @JsonProperty("last_month")
    private String lastMonth;
    @JsonProperty("currency_format")
    private Object currencyFormat;

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

    public Object getCurrencyFormat() { return currencyFormat; }
    public void setCurrencyFormat(Object currencyFormat) { this.currencyFormat = currencyFormat; }
}