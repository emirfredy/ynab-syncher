package co.personal.ynabsyncher.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Phase 1: Basic JWT Authentication (MVP)
 * 
 * Simple security configuration that:
 * - Requires authentication for all endpoints
 * - Uses JWT tokens for authentication
 * - No method-level security yet
 * - No complex authorization rules yet
 * 
 * This follows the guide's incremental approach.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll() // Health checks
                .anyRequest().authenticated() // Simple: just require authentication
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(withDefaults()) // Use Spring Boot defaults
            )
            .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // For Phase 1, use a simple mock decoder for development
        // TODO: Replace with real JWT validation in Phase 2
        return new MockJwtDecoder();
    }
    
    /**
     * Simple Mock JWT Decoder for Phase 1 development
     * Accepts any token as valid for now
     */
    private static class MockJwtDecoder implements JwtDecoder {
        @Override
        public org.springframework.security.oauth2.jwt.Jwt decode(String token) {
            return org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "read write")
                .build();
        }
    }
}