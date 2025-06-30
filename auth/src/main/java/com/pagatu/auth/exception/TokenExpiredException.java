package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class TokenExpiredException extends RuntimeException{
    private final String tokenType;

    public TokenExpiredException(String message) {
        super(message);
        this.tokenType = "UNKNOWN";
    }

    public TokenExpiredException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
        this.tokenType = "UNKNOWN";
    }

    public TokenExpiredException(String message, String tokenType, Throwable cause) {
        super(message, cause);
        this.tokenType = tokenType;
    }
}
