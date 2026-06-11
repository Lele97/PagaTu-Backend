package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when a security token is missing, unknown, or already used.
 */
@Data
public class InvalidTokenException extends RuntimeException{

    private final String tokenType;

    /**
     * @param message   error description
     * @param tokenType logical token category (e.g. RESET_TOKEN)
     */
    public InvalidTokenException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    /**
     * @param message error description
     * @param cause   underlying validation failure
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
        this.tokenType = "UNKNOWN";
    }
}
