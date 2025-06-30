package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class InvalidTokenException extends RuntimeException{
    private final String tokenType;

    public InvalidTokenException(String message) {
        super(message);
        this.tokenType = "UNKNOWN";
    }

    public InvalidTokenException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
        this.tokenType = "UNKNOWN";
    }

    public String getTokenType() {
        return tokenType;
    }
}
