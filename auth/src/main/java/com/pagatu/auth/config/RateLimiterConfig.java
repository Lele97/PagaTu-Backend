package com.pagatu.auth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration component for rate limiting functionality.
 * Provides bucket-based rate limiting using Bucket4j library with configurable
 * limits for password reset operations. Maintains separate buckets for different
 * keys (typically IP addresses or user identifiers).
 */
@Component
@Slf4j
public class RateLimiterConfig {

    @Value("${rate-limiter.reset-password.max-attempts:3}")
    private int maxAttempts;

    @Value("${rate-limiter.reset-password.time-window-hours:1}")
    private int timeWindowHours;

    @Value("${rate-limiter.reset-password.enabled:true}")
    private boolean enabled;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates a rate limiting bucket for the specified key.
     * If rate limiting is disabled, returns a bucket with unlimited capacity.
     *
     * @param key the identifier for the rate limit bucket (typically IP address)
     * @return configured Bucket instance for rate limiting
     */
    public Bucket getBucket(String key) {
        if (!enabled) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(Integer.MAX_VALUE, Refill.intervally(Integer.MAX_VALUE, Duration.ofHours(1))))
                    .build();
        }
        return buckets.computeIfAbsent(key, this::createBucket);
    }

    /**
     * Creates a new rate limit bucket with configured limits.
     *
     * @param key the identifier for the rate limit bucket
     * @return newly created Bucket with configured limits
     */
    private Bucket createBucket(String key) {
        log.debug("Creating new rate limit bucket for key: {} with {} attempts per {} hours",
                key, maxAttempts, timeWindowHours);

        Bandwidth limit = Bandwidth.classic(
                maxAttempts,
                Refill.intervally(maxAttempts, Duration.ofHours(timeWindowHours))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Removes the rate limit bucket for the specified key.
     *
     * @param key the identifier for the bucket to remove
     */
    public void removeBucket(String key) {
        buckets.remove(key);
        log.debug("Removed rate limit bucket for key: {}", key);
    }

    /**
     * Clears all rate limit buckets from the cache.
     * Useful for testing or administrative operations.
     */
    public void clearAllBuckets() {
        buckets.clear();
        log.info("Cleared all rate limit buckets");
    }

    /**
     * Returns the current number of active rate limit buckets.
     *
     * @return count of currently active buckets
     */
    public int getBucketCount() {
        return buckets.size();
    }
}