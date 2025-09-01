package com.pagatu.gateway_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = Arrays.asList(
            "http://localhost:8888",
            "https://.*.trycloudflare.com",
            "https://.*.ngrok-free.app"
    );

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

    private static CorsConfiguration getCorsConfiguration() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Use allowedOriginPatterns to support wildcards with credentials
        corsConfig.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);

        // Set allowed methods
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        // Set allowed headers
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
                "X-Reset-Token"
        ));

        // Allow credentials
        corsConfig.setAllowCredentials(true);

        // Set max age for preflight requests
        corsConfig.setMaxAge(3600L);
        return corsConfig;
    }
}