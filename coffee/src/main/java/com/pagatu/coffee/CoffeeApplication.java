package com.pagatu.coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the PagaTu Coffee payment management system.
 * <p>
 * This Spring Boot application provides a microservice for managing coffee payments
 * within groups. It handles payment registration, tracking who should pay next,
 * and maintaining payment history and statistics.
 * </p>
 * <p>
 * The application integrates with:
 * <ul>
 *   <li>Spring Cloud Discovery Client for service registration</li>
 *   <li>Apache Kafka for event-driven communication</li>
 *   <li>API Gateway for routing and security</li>
 *   <li>JWT for authentication and authorization</li>
 *   <li>JPA/Hibernate for data persistence</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CoffeeApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(CoffeeApplication.class, args);
    }
}
