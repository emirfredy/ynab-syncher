package co.personal.ynabsyncher.infrastructure.client.dto;

import java.util.List;

/**
 * YNAB API Budgets response wrapper.
 */
public class YnabBudgetsResponse {
    private YnabBudgetsData data;
    
    public YnabBudgetsData getData() { return data; }
    public void setData(YnabBudgetsData data) { this.data = data; }
    
    public static class YnabBudgetsData {
        private List<YnabBudgetDto> budgets;
        
        public List<YnabBudgetDto> getBudgets() { return budgets; }
        public void setBudgets(List<YnabBudgetDto> budgets) { this.budgets = budgets; }
    }
}