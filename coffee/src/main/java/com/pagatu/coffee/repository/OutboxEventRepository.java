package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for OutboxEvent entity.
 * Provides methods to manage the outbox queue for reliable event delivery.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find all unprocessed events ordered by creation time.
     * Limited to prevent processing too many at once.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processedAt IS NULL AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents(@Param("maxRetries") int maxRetries);

    /**
     * Find unprocessed events with limit
     */
    @Query(value = "SELECT * FROM outbox_events WHERE processed_at IS NULL AND retry_count < :maxRetries ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<OutboxEvent> findUnprocessedEventsWithLimit(@Param("maxRetries") int maxRetries, @Param("limit") int limit);

    /**
     * Delete old processed events (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processedAt IS NOT NULL AND e.processedAt < :before")
    int deleteProcessedEventsBefore(@Param("before") LocalDateTime before);

    /**
     * Count pending events
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processedAt IS NULL")
    long countPendingEvents();
}
