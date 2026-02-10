package com.pagatu.auth.service;

import com.pagatu.auth.entity.OutboxEvent;
import com.pagatu.auth.nats.NatsPublisher;
import com.pagatu.auth.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that processes the outbox table and publishes events to NATS.
 * This ensures reliable event delivery even if NATS is temporarily unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final NatsPublisher natsPublisher;

    @Value("${spring.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${spring.outbox.batch-size:100}")
    private int batchSize;

    @Value("${spring.outbox.cleanup-days:7}")
    private int cleanupDays;

    /**
     * Process pending events from the outbox and publish to NATS.
     * Runs every 5 seconds by default.
     */
    @Scheduled(fixedRateString = "${spring.outbox.poll-interval:5000}")
    @Transactional
    public void processOutbox() {
        if (!natsPublisher.isConnected()) {
            log.warn("NATS is not connected, skipping outbox processing");
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findUnprocessedEventsWithLimit(maxRetries, batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    /**
     * Process a single outbox event.
     */
    private void processEvent(OutboxEvent event) {
        try {
            // Publish to NATS
            natsPublisher.publishRaw(event.getSubject(), event.getPayload());

            // Mark as processed
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);

            log.debug("Successfully published event {} to {}", event.getId(), event.getSubject());

        } catch (Exception e) {
            // Increment retry count and save error
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(
                    e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                            : "Unknown error");
            outboxEventRepository.save(event);

            log.error("Failed to publish event {} to {} (attempt {}/{}): {}",
                    event.getId(),
                    event.getSubject(),
                    event.getRetryCount(),
                    maxRetries,
                    e.getMessage());
        }
    }

    /**
     * Cleanup old processed events.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "${spring.outbox.cleanup-cron:0 0 0 * * ?}")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        int deleted = outboxEventRepository.deleteProcessedEventsBefore(cutoffDate);

        if (deleted > 0) {
            log.info("Cleaned up {} old processed outbox events", deleted);
        }
    }

}
