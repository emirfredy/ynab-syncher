package co.personal.ynabsyncher.infrastructure.client.dto;

import java.util.List;

/**
 * YNAB API Accounts response wrapper.
 */
public class YnabAccountsResponse {
    private YnabAccountsData data;
    
    public YnabAccountsData getData() { return data; }
    public void setData(YnabAccountsData data) { this.data = data; }
    
    public static class YnabAccountsData {
        private List<YnabAccountDto> accounts;
        
        public List<YnabAccountDto> getAccounts() { return accounts; }
        public void setAccounts(List<YnabAccountDto> accounts) { this.accounts = accounts; }
    }
}