package com.pagatu.gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the API Gateway Service application.
 * <p>
 * This Spring Boot application serves as a central gateway for routing requests
 * to various microservices while providing cross-cutting concerns like
 * authentication, CORS handling, and circuit breaker fallback mechanisms.
 * </p>
 * The application integrates with:
 * <ul>
 *   <li>Service discovery integration via {@link EnableDiscoveryClient}</li>
 *   <li>JWT-based request authentication</li>
 *   <li>CORS configuration for web clients</li>
 *   <li>Route mapping to downstream microservices</li>
 *   <li>Circuit breaker fallback handling</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}
