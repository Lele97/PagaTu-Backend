package com.pagatu.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the PagaTu Authentication Service.
 * <p>
 * This Spring Boot application provides a microservice for managing user
 * authentication,
 * authorization, and registration functionality within the PagaTu ecosystem.
 * </p>
 * <p>
 * The application integrates with:
 * <ul>
 * <li>Spring Cloud Discovery Client for service registration and discovery</li>
 * <li>API Gateway for routing and security</li>
 * <li>NATS for event-driven communication</li>
 * <li>JWT (JSON Web Tokens) for secure authentication and authorization</li>
 * <li>JPA/Hibernate for data persistence with relational databases</li>
 * <li>Spring Retry for resilient operation retries</li>
 * <li>Spring Scheduling for background task execution</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableRetry
public class AuthApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
// Test trigger comment 10/16/2025 12:05:28
