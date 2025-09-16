package com.pagatu.coffee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the coffee payment application.
 * <p>
 * This configuration sets up Spring Security with stateless session management
 * suitable for a microservice architecture. The configuration:
 * <ul>
 *   <li>Disables CORS for simplicity in development</li>
 *   <li>Uses stateless session management (no server-side sessions)</li>
 *   <li>Permits all requests to coffee API endpoints and actuator endpoints</li>
 *   <li>Requires authentication for all other endpoints</li>
 * </ul>
 * </p>
 * <p>
 * The application relies on external JWT validation rather than
 * implementing authentication filters directly in this service.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Array of endpoint patterns that are permitted without authentication.
     * <p>
     * Includes actuator endpoints for monitoring, health checks, and all
     * coffee API endpoints which handle their own JWT validation.
     * </p>
     */
    private static final String[] ENDPOINTS = {
            "/actuator/**",
            "/health/**",
            "/api/coffee/**"
    };

    /**
     * Configures the security filter chain for the application.
     * <p>
     * Sets up stateless session management and defines which endpoints
     * require authentication. The configuration permits access to monitoring
     * and API endpoints while requiring authentication for others.
     * </p>
     *
     * @param http the HttpSecurity configuration object
     * @return SecurityFilterChain the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
