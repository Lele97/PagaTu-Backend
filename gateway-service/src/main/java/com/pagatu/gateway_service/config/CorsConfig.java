package com.pagatu.gateway_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration class for Cross-Origin Resource Sharing (CORS) settings.
 * Defines allowed origins, methods, headers, and other CORS-related configurations
 * for the API Gateway service.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders;

    @Value("${cors.exposed-headers:Authorization,Content-Type}")
    private String[] exposedHeaders;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    /**
     * Creates and configures a CORS web filter for reactive environments.
     * Registers CORS configuration for all endpoints.
     *
     * @return CorsWebFilter configured with allowed origins, methods, and headers
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods));
        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders));
        corsConfig.setExposedHeaders(Arrays.asList(exposedHeaders));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(maxAge);

        log.info("CORS Configuration initialized:");
        log.info("  Allowed Origins: {}", Arrays.toString(allowedOrigins));
        log.info("  Allowed Methods: {}", Arrays.toString(allowedMethods));
        log.info("  Allowed Headers: {}", Arrays.toString(allowedHeaders));
        log.info("  Allow Credentials: {}", corsConfig.getAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}