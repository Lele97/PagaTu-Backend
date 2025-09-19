package com.pagatu.gateway_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Cross-Origin Resource Sharing (CORS) settings.
 * Defines allowed origins, methods, headers, and other CORS-related configurations
 * for the API Gateway service.
 */
@Configuration
@Slf4j
public class CorsConfig {

    /**
     * List of allowed origin patterns for CORS requests.
     * Includes local development, Cloudflare, and ngrok domains with wildcard support.
     */
    private static final List<String> ALLOWED_ORIGIN_PATTERNS = Arrays.asList(
            "http://localhost:8888",
            "https://*.trycloudflare.com",
            "https://*.ngrok-free.app"
    );

    /**
     * Creates and configures a CORS web filter for reactive environments.
     * Registers CORS configuration for all endpoints.
     *
     * @return CorsWebFilter configured with allowed origins, methods, and headers
     */
    @Bean
    public CorsWebFilter corsWebFilter() {

        log.info("Configuring CORS with allowed origin patterns: {}", ALLOWED_ORIGIN_PATTERNS);

        CorsConfiguration corsConfig = getCorsConfiguration();
        log.info("CORS configuration: allowCredentials={}, allowedOriginPatterns={}",
                corsConfig.getAllowCredentials(), corsConfig.getAllowedOriginPatterns());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Creates a CORS configuration with predefined settings.
     * Configures allowed origins, methods, headers, credentials, and max age.
     *
     * @return CorsConfiguration with predefined security settings
     */
    private static CorsConfiguration getCorsConfiguration() {

        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);

        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Cache-Control",
                "Content-Type",
                "X-Requested-With",
                "ngrok-skip-browser-warning",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Access-Control-Allow-Origin",
                "X-Reset-Token"
        ));

        corsConfig.setAllowCredentials(true);

        corsConfig.setMaxAge(3600L);

        return corsConfig;
    }
}