package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when a client exceeds an allowed request rate.
 */
@Data
public class RateLimiterException extends RuntimeException{

    private final long waitTimeSeconds;
    private final String clientIdentifier;

    /**
     * @param message            error description
     * @param waitTimeSeconds    suggested retry delay in seconds
     * @param clientIdentifier   client key that triggered the limiter
     */
    public RateLimiterException(String message, long waitTimeSeconds, String clientIdentifier) {
        super(message);
        this.waitTimeSeconds = waitTimeSeconds;
        this.clientIdentifier = clientIdentifier;
    }
}
