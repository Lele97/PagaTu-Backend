package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class RateLimiterException extends RuntimeException{

    private final long waitTimeSeconds;
    private final String clientIdentifier;

    public RateLimiterException(String message, long waitTimeSeconds, String clientIdentifier) {
        super(message);
        this.waitTimeSeconds = waitTimeSeconds;
        this.clientIdentifier = clientIdentifier;
    }
}
