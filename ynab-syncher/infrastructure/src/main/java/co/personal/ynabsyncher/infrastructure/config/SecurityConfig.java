package co.personal.ynabsyncher.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 2: Method-Level Security with @PreAuthorize
 * 
 * Enhanced security configuration that:
 * - Requires authentication for all endpoints  
 * - Uses JWT tokens with role-based claims
 * - Enables method-level security with @PreAuthorize
 * - Supports ADMIN, USER, and READ_ONLY roles
 * 
 * This builds on Phase 1 with fine-grained authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize support
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
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Map JWT claims to authorities
                )
            )
            .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }
    
    /**
     * Extract authorities from JWT claims for @PreAuthorize
     * Maps the 'authorities' claim to Spring Security GrantedAuthority objects
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract roles from JWT claims and map to Spring Security authorities
        Object rolesObj = jwt.getClaim("roles");
        System.out.println("DEBUG: JWT claims: " + jwt.getClaims());
        System.out.println("DEBUG: Roles claim: " + rolesObj);
        
        if (rolesObj instanceof List<?> rolesList) {
            Collection<GrantedAuthority> authorities = rolesList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> "ROLE_" + role) // Add ROLE_ prefix for Spring Security
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
            System.out.println("DEBUG: Extracted authorities: " + authorities);
            return authorities;
        }
        System.out.println("DEBUG: No roles found, returning empty authorities");
        return List.of(); // No authorities if claim is missing
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Phase 2: Enhanced mock decoder with role-based claims
        // TODO: Replace with real JWT validation in Phase 3
        return new MockJwtDecoder();
    }
    
    /**
     * Enhanced Mock JWT Decoder for Phase 2 development
     * Provides role-based claims for @PreAuthorize testing
     * 
     * Token format determines role:
     * - "admin-token" -> ADMIN role
     * - "user-token" -> USER role  
     * - "readonly-token" -> READ_ONLY role
     * - anything else -> USER role (default)
     */
    private static class MockJwtDecoder implements JwtDecoder {
        @Override
        public org.springframework.security.oauth2.jwt.Jwt decode(String token) {
            // Determine roles based on token value for testing
            java.util.List<String> roles;
            
            if ("admin-token".equals(token)) {
                roles = java.util.List.of("ADMIN", "USER", "READ_ONLY");
            } else if ("readonly-token".equals(token)) {
                roles = java.util.List.of("READ_ONLY");
            } else {
                // Default to USER role (includes user-token and any other token)
                roles = java.util.List.of("USER", "READ_ONLY");
            }
            
            return org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "read write")
                .claim("roles", roles)
                .build();
        }
    }
}