package co.personal.ynabsyncher.infrastructure.client;

import co.personal.ynabsyncher.api.error.YnabApiException;
import co.personal.ynabsyncher.infrastructure.client.dto.*;
import co.personal.ynabsyncher.model.ynab.YnabAccount;
import co.personal.ynabsyncher.model.ynab.YnabBudget;
import co.personal.ynabsyncher.model.ynab.YnabCategory;
import co.personal.ynabsyncher.model.ynab.YnabTransaction;
import co.personal.ynabsyncher.spi.client.YnabApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring-based implementation of YnabApiClient using RestTemplate.
 * Handles authentication, rate limiting, error handling, and data conversion.
 */
@Service
public class YnabApiClientImpl implements YnabApiClient {
    private static final Logger logger = LoggerFactory.getLogger(YnabApiClientImpl.class);
    private static final String API_VERSION = "v1";
    private static final String USER_AGENT = "YNAB-Syncher/1.0";
    private static final String CORRELATION_ID_KEY = "correlationId";
    
    private final RestTemplate restTemplate;
    private final YnabApiMapper mapper;
    private final String baseUrl;
    private final String accessToken;

    public YnabApiClientImpl(
            RestTemplate restTemplate,
            YnabApiMapper mapper,
            @Value("${ynab.api.base-url:https://api.ynab.com}") String baseUrl,
            @Value("${ynab.api.access-token}") String accessToken) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "YnabApiMapper cannot be null");
        this.baseUrl = validateAndCleanBaseUrl(baseUrl);
        this.accessToken = validateAccessToken(accessToken);
    }

    @Override
    public List<YnabBudget> getBudgets() {
        return executeWithCorrelation("getBudgets", () -> {
            logger.debug("Fetching all budgets from YNAB API");
            
            ResponseEntity<YnabBudgetsResponse> response = executeRequest(
                    "/budgets", 
                    HttpMethod.GET, 
                    null, 
                    YnabBudgetsResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabBudgetsResponse::getData)
                    .map(YnabBudgetsResponse.YnabBudgetsData::getBudgets)
                    .orElse(List.of())
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        });
    }

    @Override
    public Optional<YnabBudget> getBudget(String budgetId) {
        validateBudgetId(budgetId);
        
        return executeWithCorrelation("getBudget", () -> {
            logger.debug("Fetching budget: {}", budgetId);
            
            try {
                ResponseEntity<YnabBudgetResponse> response = executeRequest(
                        "/budgets/" + budgetId, 
                        HttpMethod.GET, 
                        null, 
                        YnabBudgetResponse.class
                );
                
                return Optional.ofNullable(response.getBody())
                        .map(YnabBudgetResponse::getData)
                        .map(YnabBudgetResponse.YnabBudgetData::getBudget)
                        .map(mapper::toDomain);
                        
            } catch (HttpClientErrorException httpError) {
                if (httpError.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.debug("Budget not found: {}", budgetId);
                    return Optional.empty();
                }
                throw mapToYnabApiException("Failed to fetch budget: " + budgetId, httpError);
            }
        });
    }

    @Override
    public List<YnabAccount> getAccounts(String budgetId) {
        validateBudgetId(budgetId);
        
        return executeWithCorrelation("getAccounts", () -> {
            logger.debug("Fetching accounts for budget: {}", budgetId);
            
            ResponseEntity<YnabAccountsResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/accounts", 
                    HttpMethod.GET, 
                    null, 
                    YnabAccountsResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabAccountsResponse::getData)
                    .map(YnabAccountsResponse.YnabAccountsData::getAccounts)
                    .orElse(List.of())
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        });
    }

    @Override
    public List<YnabCategory> getCategories(String budgetId) {
        validateBudgetId(budgetId);
        
        return executeWithCorrelation("getCategories", () -> {
            logger.debug("Fetching categories for budget: {}", budgetId);
            
            ResponseEntity<YnabCategoriesResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/categories", 
                    HttpMethod.GET, 
                    null, 
                    YnabCategoriesResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabCategoriesResponse::getData)
                    .map(YnabCategoriesResponse.YnabCategoriesData::getCategoryGroups)
                    .orElse(List.of())
                    .stream()
                    .flatMap(group -> group.getCategories().stream()
                        .map(category -> mapper.toDomain(category, group.getName())))
                    .toList();
        });
    }

    @Override
    public List<YnabTransaction> getTransactions(String budgetId) {
        validateBudgetId(budgetId);
        
        return executeWithCorrelation("getTransactions", () -> {
            logger.debug("Fetching transactions for budget: {}", budgetId);
            
            ResponseEntity<YnabTransactionsResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/transactions", 
                    HttpMethod.GET, 
                    null, 
                    YnabTransactionsResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabTransactionsResponse::getData)
                    .map(YnabTransactionsResponse.YnabTransactionsData::getTransactions)
                    .orElse(List.of())
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        });
    }

    @Override
    public List<YnabTransaction> getTransactionsSince(String budgetId, OffsetDateTime sinceDate) {
        validateBudgetId(budgetId);
        if (sinceDate == null) {
            throw new IllegalArgumentException("Since date cannot be null");
        }
        
        String formattedDate = sinceDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        return executeWithCorrelation("getTransactionsSince", () -> {
            logger.debug("Fetching transactions for budget: {} since: {}", budgetId, formattedDate);
            
            ResponseEntity<YnabTransactionsResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/transactions?since_date=" + formattedDate, 
                    HttpMethod.GET, 
                    null, 
                    YnabTransactionsResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabTransactionsResponse::getData)
                    .map(YnabTransactionsResponse.YnabTransactionsData::getTransactions)
                    .orElse(List.of())
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        });
    }

    @Override
    public List<YnabTransaction> getAccountTransactions(String budgetId, String accountId) {
        validateBudgetId(budgetId);
        validateAccountId(accountId);
        
        return executeWithCorrelation("getAccountTransactions", () -> {
            logger.debug("Fetching transactions for budget: {} account: {}", budgetId, accountId);
            
            ResponseEntity<YnabTransactionsResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/accounts/" + accountId + "/transactions", 
                    HttpMethod.GET, 
                    null, 
                    YnabTransactionsResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabTransactionsResponse::getData)
                    .map(YnabTransactionsResponse.YnabTransactionsData::getTransactions)
                    .orElse(List.of())
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        });
    }

    @Override
    public YnabTransaction createTransaction(String budgetId, YnabTransaction transaction) {
        validateBudgetId(budgetId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return executeWithCorrelation("createTransaction", () -> {
            logger.debug("Creating transaction in budget: {} for account: {}", budgetId, transaction.accountId());
            
            YnabTransactionCreateRequest request = new YnabTransactionCreateRequest(
                    mapper.toApi(transaction)
            );
            
            ResponseEntity<YnabTransactionResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/transactions", 
                    HttpMethod.POST, 
                    request, 
                    YnabTransactionResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabTransactionResponse::getData)
                    .map(YnabTransactionResponse.YnabTransactionData::getTransaction)
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new YnabApiException("Empty response for transaction creation", new RuntimeException("No data in response")));
        });
    }

    @Override
    public YnabTransaction updateTransaction(String budgetId, String transactionId, YnabTransaction transaction) {
        validateBudgetId(budgetId);
        validateTransactionId(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return executeWithCorrelation("updateTransaction", () -> {
            logger.debug("Updating transaction: {} in budget: {}", transactionId, budgetId);
            
            YnabTransactionUpdateRequest request = new YnabTransactionUpdateRequest(
                    mapper.toApi(transaction)
            );
            
            ResponseEntity<YnabTransactionResponse> response = executeRequest(
                    "/budgets/" + budgetId + "/transactions/" + transactionId, 
                    HttpMethod.PUT, 
                    request, 
                    YnabTransactionResponse.class
            );
            
            return Optional.ofNullable(response.getBody())
                    .map(YnabTransactionResponse::getData)
                    .map(YnabTransactionResponse.YnabTransactionData::getTransaction)
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new YnabApiException("Empty response for transaction update", new RuntimeException("No data in response")));
        });
    }

    @Override
    public boolean isHealthy() {
        try {
            getBudgets();
            logger.info("YNAB API health check successful");
            return true;
        } catch (Exception e) {
            logger.warn("YNAB API health check failed", e);
            return false;
        }
    }

    private <T> ResponseEntity<T> executeRequest(String path, HttpMethod method, Object body, Class<T> responseType) {
        String url = baseUrl + "/" + API_VERSION + path;
        HttpHeaders headers = createHeaders();
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        
        logger.debug("Executing YNAB API request: {} {}", method, url);
        
        try {
            return restTemplate.exchange(url, method, entity, responseType);
        } catch (HttpClientErrorException e) {
            logger.error("YNAB API client error: {} {}, Status: {}, Response: {}", 
                    method, url, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("YNAB API connection error: {} {}", method, url, e);
            throw e;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private <T> T executeWithCorrelation(String operation, YnabOperation<T> operationFunc) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_KEY, correlationId);
        
        try {
            logger.debug("Starting YNAB operation: {} [correlationId={}]", operation, correlationId);
            T result = operationFunc.execute();
            logger.debug("Completed YNAB operation: {} [correlationId={}]", operation, correlationId);
            return result;
        } catch (Exception e) {
            logger.error("Failed YNAB operation: {} [correlationId={}]", operation, correlationId, e);
            throw mapToYnabApiException("YNAB API operation failed: " + operation, e);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private YnabApiException mapToYnabApiException(String message, Exception cause) {
        if (cause instanceof HttpClientErrorException httpError) {
            String errorBody = httpError.getResponseBodyAsString();
            logger.error("YNAB API HTTP error: {} - Status: {}, Body: {}", message, httpError.getStatusCode(), errorBody);
            
            return new YnabApiException(
                    message + ": " + httpError.getMessage(),
                    httpError.getStatusCode().value(),
                    null, // errorId would be parsed from response JSON
                    httpError.getStatusCode().toString(),
                    errorBody
            );
        }
        
        if (cause instanceof ResourceAccessException) {
            logger.error("YNAB API connection error: {}", message, cause);
            return new YnabApiException("YNAB API connection failed: " + message, cause);
        }
        
        logger.error("YNAB API unexpected error: {}", message, cause);
        return new YnabApiException(message, cause);
    }

    private String validateAndCleanBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("YNAB API base URL cannot be null or empty");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("YNAB access token must be provided");
        }
        return accessToken;
    }

    private void validateBudgetId(String budgetId) {
        if (budgetId == null || budgetId.isBlank()) {
            throw new IllegalArgumentException("Budget ID cannot be null or empty");
        }
    }

    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
    }

    private void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
    }

    @FunctionalInterface
    private interface YnabOperation<T> {
        T execute() throws Exception;
    }
}