package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an event in the outbox table.
 * Used for the Transactional Outbox Pattern to ensure
 * reliable event delivery to NATS without requiring
 * persistent volumes.
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The NATS subject/topic to publish to
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * The event type (class name) for deserialization
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * The serialized event payload (JSON)
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * When the event was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the event was successfully published to NATS
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Last error message if publishing failed
     */
    @Column(name = "last_error", length = 500)
    private String lastError;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
