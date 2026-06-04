package com.pagatu.mail.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NatsSubscriber {

    private final Connection natsConnection;
    private Dispatcher dispatcher;

    public NatsSubscriber(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    @PostConstruct
    public void init() {
        dispatcher = natsConnection.createDispatcher();
    }

    /**
     * Subscribe to a subject with a handler
     */
    public void subscribe(String subject, MessageHandler handler) {
        dispatcher.subscribe(subject, handler);
    }

    @PreDestroy
    public void cleanup() throws InterruptedException {
        if (dispatcher != null) {
            dispatcher.drain(java.time.Duration.ofSeconds(5));
        }
    }
}
