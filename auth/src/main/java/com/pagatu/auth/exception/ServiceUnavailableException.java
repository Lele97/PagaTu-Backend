package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when a downstream service call fails.
 */
@Data
public class ServiceUnavailableException extends RuntimeException{

    private final String serviceName;

    /**
     * @param message error description
     * @param cause   underlying remote service failure
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = "UNKNOWN";
    }
}
