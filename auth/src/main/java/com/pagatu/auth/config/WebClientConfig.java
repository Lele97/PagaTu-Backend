package com.pagatu.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for WebClient setup.
 * Provides a WebClient builder bean for making HTTP requests
 * in a reactive non-blocking manner.
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates a WebClient builder bean for building WebClient instances.
     *
     * @return WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}