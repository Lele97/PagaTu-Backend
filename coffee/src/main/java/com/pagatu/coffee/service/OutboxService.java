package com.pagatu.coffee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagatu.coffee.entity.OutboxEvent;
import com.pagatu.coffee.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing the outbox pattern.
 * Saves events to the outbox table within the same transaction
 * as the business logic, ensuring atomicity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save an event to the outbox table.
     * This should be called within the same transaction as the business logic.
     *
     * @param subject   The NATS subject to publish to
     * @param eventType The event class name (for logging/debugging)
     * @param event     The event object to serialize and save
     */
    @Transactional
    public void saveEvent(String subject, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .subject(subject)
                    .eventType(eventType)
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Saved event to outbox: subject={}, type={}", subject, eventType);

        } catch (Exception e) {
            log.error("Failed to save event to outbox: subject={}, type={}", subject, eventType, e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    /**
     * Save an event with a specific class type.
     *
     * @param subject The NATS subject to publish to
     * @param event   The event object
     * @param <T>     The event type
     */
    @Transactional
    public <T> void saveEvent(String subject, T event) {
        saveEvent(subject, event.getClass().getSimpleName(), event);
    }

    /**
     * Get count of pending events in the outbox.
     *
     * @return Number of unprocessed events
     */
    public long getPendingEventCount() {
        return outboxEventRepository.countPendingEvents();
    }
}
