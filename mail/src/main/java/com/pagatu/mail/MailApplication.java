package com.pagatu.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *
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
