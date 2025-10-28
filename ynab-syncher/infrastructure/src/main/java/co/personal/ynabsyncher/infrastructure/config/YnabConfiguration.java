package co.personal.ynabsyncher.infrastructure.config;

import co.personal.ynabsyncher.infrastructure.client.YnabApiClientImpl;
import co.personal.ynabsyncher.infrastructure.client.YnabApiMapper;
import co.personal.ynabsyncher.spi.client.YnabApiClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for YNAB API integration.
 * Provides beans for YNAB API client, RestTemplate, and configuration properties.
 */
@Configuration
@EnableConfigurationProperties(YnabConfiguration.YnabProperties.class)
public class YnabConfiguration {

    /**
     * Creates a RestTemplate bean configured for YNAB API requests.
     * Includes timeout settings and error handling.
     */
    @Bean
    public RestTemplate ynabRestTemplate(YnabProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        
        return new RestTemplate(factory);
    }

    /**
     * Creates the YNAB API client implementation.
     */
    @Bean
    public YnabApiClient ynabApiClient(RestTemplate ynabRestTemplate, YnabApiMapper mapper, YnabProperties properties) {
        return new YnabApiClientImpl(ynabRestTemplate, mapper, properties.getBaseUrl(), properties.getAccessToken());
    }

    /**
     * YNAB API configuration properties.
     * Validates required properties and provides defaults for optional ones.
     */
    @ConfigurationProperties(prefix = "ynab.api")
    public static class YnabProperties {
        
        /**
         * YNAB API base URL. Defaults to the official YNAB API.
         */
        private String baseUrl = "https://api.ynab.com";
        
        /**
         * YNAB Personal Access Token for authentication.
         * Required for all API operations.
         */
        private String accessToken;
        
        /**
         * Default budget ID to use when none is specified.
         * If not provided, the first available budget will be used.
         */
        private String defaultBudgetId;
        
        /**
         * Connection timeout in milliseconds.
         */
        private int connectTimeoutMs = 5000;
        
        /**
         * Read timeout in milliseconds.
         */
        private int readTimeoutMs = 30000;
        
        /**
         * Enable rate limiting to respect YNAB's 200 requests/hour limit.
         */
        private boolean rateLimitingEnabled = true;
        
        /**
         * Maximum requests per hour (YNAB's limit is 200).
         */
        private int maxRequestsPerHour = 200;
        
        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        
        public String getDefaultBudgetId() { return defaultBudgetId; }
        public void setDefaultBudgetId(String defaultBudgetId) { this.defaultBudgetId = defaultBudgetId; }
        
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        
        public boolean isRateLimitingEnabled() { return rateLimitingEnabled; }
        public void setRateLimitingEnabled(boolean rateLimitingEnabled) { this.rateLimitingEnabled = rateLimitingEnabled; }
        
        public int getMaxRequestsPerHour() { return maxRequestsPerHour; }
        public void setMaxRequestsPerHour(int maxRequestsPerHour) { this.maxRequestsPerHour = maxRequestsPerHour; }
    }
}