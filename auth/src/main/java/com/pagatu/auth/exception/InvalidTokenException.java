package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class InvalidTokenException extends RuntimeException{

    private final String tokenType;

    public InvalidTokenException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
        this.tokenType = "UNKNOWN";
    }
}
