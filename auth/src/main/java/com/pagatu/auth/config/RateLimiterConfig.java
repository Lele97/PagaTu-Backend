package com.pagatu.auth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

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
     * Get or create a bucket for the given key (typically IP address)
     */
    public Bucket getBucket(String key) {
        if (!enabled) {
            // If rate limiting is disabled, return a bucket that always allows requests
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(Integer.MAX_VALUE, Refill.intervally(Integer.MAX_VALUE, Duration.ofHours(1))))
                    .build();
        }

        return buckets.computeIfAbsent(key, this::createBucket);
    }

    /**
     * Create a new bucket with the configured limits
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
     * Remove bucket for a key (useful for cleanup)
     */
    public void removeBucket(String key) {
        buckets.remove(key);
        log.debug("Removed rate limit bucket for key: {}", key);
    }

    /**
     * Clear all buckets (useful for testing or admin operations)
     */
    public void clearAllBuckets() {
        buckets.clear();
        log.info("Cleared all rate limit buckets");
    }

    /**
     * Get current bucket count (useful for monitoring)
     */
    public int getBucketCount() {
        return buckets.size();
    }
}
