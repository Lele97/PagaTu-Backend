package com.pagatu.mail.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * NATS message subscriber for the mail service.
 * <p>
 * Manages a single {@link Dispatcher} used to register subject handlers
 * and gracefully drain pending messages on shutdown.
 * </p>
 */
@Component
public class NatsSubscriber {

    private final Connection natsConnection;
    private Dispatcher dispatcher;

    /**
     * @param natsConnection active NATS connection bean
     */
    public NatsSubscriber(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    /**
     * Creates the NATS dispatcher on application startup.
     */
    @PostConstruct
    public void init() {
        dispatcher = natsConnection.createDispatcher();
    }

    /**
     * Registers a handler for the given NATS subject.
     *
     * @param subject NATS subject name
     * @param handler callback invoked for each incoming message
     */
    public void subscribe(String subject, MessageHandler handler) {
        dispatcher.subscribe(subject, handler);
    }

    /**
     * Drains the dispatcher before shutdown to avoid losing in-flight messages.
     */
    @PreDestroy
    public void cleanup() throws InterruptedException {
        if (dispatcher != null) {
            dispatcher.drain(java.time.Duration.ofSeconds(5));
        }
    }
}
