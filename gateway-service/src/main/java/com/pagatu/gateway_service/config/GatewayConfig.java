package com.pagatu.gateway_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for defining API gateway routes.
 * Maps incoming requests to appropriate microservices and configures circuit breakers
 * for fault tolerance.
 */
@Configuration
public class GatewayConfig {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${coffee.service.url}")
    private String coffeeServiceUrl;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    /**
     * Configures the route mappings for different microservices.
     * Sets up path-based routing with circuit breaker fallback mechanisms.
     *
     * @param builder RouteLocatorBuilder for creating route configurations
     * @return RouteLocator with configured routes and fallback handlers
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth")))
                        .uri(authServiceUrl))
                .route("coffee-service", r -> r.path("/api/coffee/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("coffeeCircuitBreaker")
                                .setFallbackUri("forward:/fallback/coffee")))
                        .uri(coffeeServiceUrl))
                .route("email-service", r -> r.path("/api/email/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("emailCircuitBreaker")
                                .setFallbackUri("forward:/fallback/mail")))
                        .uri(emailServiceUrl))
                .build();
    }
}