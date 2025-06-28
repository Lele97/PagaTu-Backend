package com.pagatu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimiterResult {

    private boolean allowed;
    private long remainingTokens;
    private long waitTimeSeconds;

    /**
     * Create a result indicating the request is allowed
     */
    public static RateLimiterResult allowed(long remainingTokens) {
        return new RateLimiterResult(true, remainingTokens, 0);
    }

    /**
     * Create a result indicating the request is denied
     */
    public static RateLimiterResult denied(long waitTimeSeconds) {
        return new RateLimiterResult(false, 0, waitTimeSeconds);
    }
}
