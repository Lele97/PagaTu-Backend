package com.pagatu.auth.service;

import com.pagatu.auth.config.RateLimiterConfig;
import com.pagatu.auth.entity.RateLimiterResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service class for handling rate limiting operations.
 * Provides functionality to check and enforce rate limits using token bucket algorithm.
 * Integrates with RateLimiterConfig to manage rate limiting buckets for different clients.
 */
@Service
@Log4j2
public class RateLimiterService {

    private final RateLimiterConfig rateLimiterConfig;

    /**
     * Constructs a RateLimiterService with the required configuration.
     *
     * @param rateLimiterConfig the rate limiter configuration providing bucket management
     */
    public RateLimiterService(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    /**
     * Checks if a request is allowed based on rate limiting rules for the specified key.
     * Uses a token bucket algorithm to determine if the request can be processed immediately
     * or should be rate limited with a suggested wait time.
     *
     * @param key the unique identifier for rate limiting (typically client IP address)
     * @return RateLimiterResult containing whether the request is allowed, remaining tokens,
     *         and wait time in seconds if rate limited
     */
    public RateLimiterResult checkRateLimit(String key) {
        Bucket bucket = rateLimiterConfig.getBucket(key);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            long remainingTokens = probe.getRemainingTokens();
            return new RateLimiterResult(true, remainingTokens, 0);
        } else {
            long nanosToWait = probe.getNanosToWaitForRefill();
            long waitTimeSeconds = Duration.ofNanos(nanosToWait).getSeconds();
            return new RateLimiterResult(false, 0, waitTimeSeconds);
        }
    }
}