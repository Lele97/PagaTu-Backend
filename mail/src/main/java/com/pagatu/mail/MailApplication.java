package com.pagatu.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the Paga-Tu Mail Service application.
 * <p>
 * This Spring Boot application handles email notifications for various events
 * in the Paga-Tu ecosystem,
 * including payment notifications, group invitations, and password reset
 * requests.
 * </p>
 * <p>
 * The application integrates with:
 * <ul>
 * <li>Spring Cloud Discovery Client for service registration</li>
 * <li>Email notification system for payment rotations</li>
 * <li>Group invitation management</li>
 * <li>Password reset functionality</li>
 * <li>Integration with other microservices via WebClient</li>
 * <li>NATS-based event consumption</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MailApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(MailApplication.class, args);
    }
}
