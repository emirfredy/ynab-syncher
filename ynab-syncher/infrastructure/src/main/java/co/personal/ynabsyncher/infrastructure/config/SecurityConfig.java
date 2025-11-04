package co.personal.ynabsyncher.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for YNAB Syncher
 * 
 * Supports multiple authentication modes based on Spring profiles:
 * - Default profile: No authentication (development mode)
 * - Docker profile: OAuth2 JWT validation ready (but initially disabled)
 * 
 * This follows hexagonal architecture principles where authentication
 * is purely an infrastructure concern and doesn't affect the domain layer.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize annotations
public class SecurityConfig {

    /**
     * Development Security Configuration (Default Profile)
     * 
     * No authentication required - permits all requests for rapid development.
     * Used with H2 database and local development workflow.
     */
    @Configuration
    @ConditionalOnProperty(
        name = "app.auth.external-validation.enabled", 
        havingValue = "false", 
        matchIfMissing = true
    )
    @Order(1)
    static class DevelopmentSecurityConfig {

        @Bean
        public SecurityFilterChain developmentFilterChain(HttpSecurity http) throws Exception {
            return http
                // Disable CSRF for API-only application
                .csrf(AbstractHttpConfigurer::disable)
                
                // Disable CORS (will be configured later if needed)
                .cors(AbstractHttpConfigurer::disable)
                
                // Permit all requests (no authentication)
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                )
                
                // Stateless session (no server-side sessions)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Disable default login form
                .formLogin(AbstractHttpConfigurer::disable)
                
                // Disable HTTP Basic authentication
                .httpBasic(AbstractHttpConfigurer::disable)
                
                .build();
        }
    }

    /**
     * OAuth2 Security Configuration (Docker Profile)
     * 
     * Ready for JWT validation but initially disabled via configuration.
     * When enabled, validates JWT tokens from Keycloak/external OAuth2 provider.
     */
    @Configuration
    @ConditionalOnProperty(
        name = "app.auth.external-validation.enabled", 
        havingValue = "true"
    )
    @Order(2)
    static class OAuth2SecurityConfig {

        @Bean
        public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
            return http
                // Disable CSRF for API-only application
                .csrf(AbstractHttpConfigurer::disable)
                
                // Disable CORS (will be configured later if needed)
                .cors(AbstractHttpConfigurer::disable)
                
                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> { /* JWT decoder auto-configured from issuer-uri */ })
                )
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                    // Public endpoints (no authentication required)
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/metrics",
                        "/h2-console/**"  // H2 console for development
                    ).permitAll()
                    
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
                )
                
                // Stateless session (JWT-based authentication)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Disable default login form (OAuth2/JWT only)
                .formLogin(AbstractHttpConfigurer::disable)
                
                // Disable HTTP Basic authentication (OAuth2/JWT only)
                .httpBasic(AbstractHttpConfigurer::disable)
                
                .build();
        }
    }
}