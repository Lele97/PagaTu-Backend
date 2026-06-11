package com.pagatu.auth.exception;

/**
 * Exception thrown when login credentials are invalid.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * @param message authentication failure description
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
