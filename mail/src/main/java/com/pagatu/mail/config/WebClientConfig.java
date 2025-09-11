package com.pagatu.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for WebClient beans.
 * <p>
 * This configuration provides the necessary WebClient.Builder bean
 * that can be used throughout the application for making HTTP requests
 * to external services.
 * </p>
 * <p>
 * The WebClient.Builder is configured as a bean to allow for potential
 * customization and to ensure consistent configuration across all
 * WebClient instances created in the application.
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates and configures a WebClient.Builder bean.
     * <p>
     * This builder can be used to create WebClient instances with
     * custom configurations such as base URLs, timeouts, and other
     * HTTP client settings.
     * </p>
     * <p>
     * The builder is provided as a bean to enable dependency injection
     * and to ensure consistent WebClient configuration across the
     * application.
     * </p>
     *
     * @return a configured WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}