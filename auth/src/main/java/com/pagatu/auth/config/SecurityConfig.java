package com.pagatu.auth.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration class for authentication service.
 * Configures HTTP security, CORS, session management, and password encoding.
 * Defines public endpoints and security policies for the application.
 */
@Configuration
@EnableWebSecurity
@Log4j2
public class SecurityConfig {

    /**
     * List of whitelisted paths that are excluded from authentication.
     * <p>
     * Includes authentication endpoints, API documentation (Swagger), and
     * development tools (e.g., H2 console).
     */
    private static final String[] AUTH_WHITELIST = {
            "/api/auth/**",
            "/v3/api-docs/*",
            "/swagger-ui/*",
            "/swagger-ui.html",
            "/h2-console/**",
    };

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * @param http the HttpSecurity to modify
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Provides a password encoder bean for hashing and verifying passwords.
     *
     * @return BCryptPasswordEncoder instance for password encoding
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
