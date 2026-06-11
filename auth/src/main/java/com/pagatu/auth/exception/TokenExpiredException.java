package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when a security token has expired.
 */
@Data
public class TokenExpiredException extends RuntimeException{

    private final String tokenType;

    /**
     * @param message   error description
     * @param tokenType logical token category (e.g. RESET_TOKEN)
     */
    public TokenExpiredException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }
}
