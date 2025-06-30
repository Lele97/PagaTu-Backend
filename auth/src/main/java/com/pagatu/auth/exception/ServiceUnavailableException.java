package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class ServiceUnavailableException extends RuntimeException{
    private final String serviceName;

    public ServiceUnavailableException(String message) {
        super(message);
        this.serviceName = "UNKNOWN";
    }

    public ServiceUnavailableException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = "UNKNOWN";
    }
}
