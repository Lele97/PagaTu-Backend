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

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:8888",
            "https://3565-37-118-134-2.ngrok-free.app",
            "https://lens-drinking-dec-note.trycloudflare.com",
            "https://9f919b1ef69d.ngrok-free.app"
    );

    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Configuring CORS with allowed origins: {}", ALLOWED_ORIGINS);

        CorsConfiguration corsConfig = getCorsConfiguration();

        // DON'T call applyPermitDefaultValues() as it adds wildcards
        log.info("CORS configuration: allowCredentials={}, allowedOrigins={}",
                corsConfig.getAllowCredentials(), corsConfig.getAllowedOrigins());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        // Make sure CORS filter has highest precedence
        return new CorsWebFilter(source);
    }

    private static CorsConfiguration getCorsConfiguration() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Explicitly set allowed origins (no wildcards when allowCredentials is true)
        corsConfig.setAllowedOrigins(ALLOWED_ORIGINS);

        corsConfig.setAllowedOriginPatterns(ALLOWED_ORIGINS);

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