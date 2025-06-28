package com.pagatu.auth.service;

import com.pagatu.auth.config.RateLimiterConfig;
import com.pagatu.auth.entity.RateLimiterResult;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Log4j2
public class RateLimiterService {

    private final RateLimiterConfig rateLimiterConfig;

    public RateLimiterService(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    /**
     * Checks if the request is allowed based on rate limiting rules
     *
     * @param key The unique identifier (usually IP address)
     * @return RateLimitResult containing whether request is allowed and remaining tokens
     */
    public RateLimiterResult checkRateLimit(String key) {
        Bucket bucket = rateLimiterConfig.getBucket(key);

        // Prova a consumare un token e ottieni un probe
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Token consumato con successo
            long remainingTokens = probe.getRemainingTokens();
            return new RateLimiterResult(true, remainingTokens, 0);
        } else {
            // Nessun token disponibile, calcola tempo di attesa
            long nanosToWait = probe.getNanosToWaitForRefill();
            long waitTimeSeconds = Duration.ofNanos(nanosToWait).getSeconds();
            return new RateLimiterResult(false, 0, waitTimeSeconds);
        }
    }
}
