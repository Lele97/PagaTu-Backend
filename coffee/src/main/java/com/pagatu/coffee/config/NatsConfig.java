package com.pagatu.coffee.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Configuration
@Slf4j
public class NatsConfig {

    @Value("${spring.nats.server:nats://localhost:4222}")
    private String natsServerUrl;

    @Value("${spring.nats.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${spring.nats.connection-name:coffee-service}")
    private String connectionName;

    @Bean
    public Connection natsConnection() {
        try {
            Options options = Options.builder().server(natsServerUrl).connectionName(connectionName)
                    .connectionTimeout(Duration.ofMillis(connectionTimeout)).reconnectWait(Duration.ofSeconds(2))
                    .maxReconnects(-1) // Infinite reconnects
                    .pingInterval(Duration.ofSeconds(30)).build();

            Connection connection = Nats.connect(options);
            log.info("Connected to NATS server: {}", natsServerUrl);
            return connection;

        } catch (Exception e) {
            log.error("Failed to connect to NATS server: {}", natsServerUrl, e);
            throw new RuntimeException("Failed to connect to NATS", e);
        }
    }
}
