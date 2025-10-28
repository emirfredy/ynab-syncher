package co.personal.ynabsyncher.infrastructure.client.dto;

/**
 * YNAB API Budget response wrapper.
 */
public class YnabBudgetResponse {
    private YnabBudgetData data;
    
    public YnabBudgetData getData() { return data; }
    public void setData(YnabBudgetData data) { this.data = data; }
    
    public static class YnabBudgetData {
        private YnabBudgetDto budget;
        
        public YnabBudgetDto getBudget() { return budget; }
        public void setBudget(YnabBudgetDto budget) { this.budget = budget; }
    }
}