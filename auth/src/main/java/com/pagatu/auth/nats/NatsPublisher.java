package com.pagatu.auth.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * NATS Core publisher for sending messages.
 * Supports both object serialization and raw string publishing.
 */
@Service
@Slf4j
public class NatsPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public NatsPublisher(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish an object as JSON to a NATS subject.
     *
     * @param subject The subject/topic to publish to
     * @param event   The event object to serialize
     */
    public void publish(String subject, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            natsConnection.publish(subject, json.getBytes(StandardCharsets.UTF_8));
            log.debug("Published event to subject: {}", subject);
        } catch (Exception e) {
            log.error("Failed to publish message to {}", subject, e);
            throw new RuntimeException("Failed to publish message to " + subject, e);
        }
    }

    /**
     * Publish a raw string message (already serialized JSON).
     * Used by OutboxProcessor.
     *
     * @param subject The subject/topic to publish to
     * @param payload The already-serialized JSON payload
     */
    public void publishRaw(String subject, String payload) {
        try {
            natsConnection.publish(subject, payload.getBytes(StandardCharsets.UTF_8));
            log.debug("Published raw message to subject: {}", subject);
        } catch (Exception e) {
            log.error("Failed to publish raw message to {}", subject, e);
            throw new RuntimeException("Failed to publish message to " + subject, e);
        }
    }

    /**
     * Check if the NATS connection is active.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return natsConnection.getStatus() == Connection.Status.CONNECTED;
    }
}
