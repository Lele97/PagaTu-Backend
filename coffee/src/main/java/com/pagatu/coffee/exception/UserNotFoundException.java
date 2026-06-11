package com.pagatu.coffee.exception;

/**
 * Exception thrown when a coffee user cannot be resolved by auth ID or username.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * @param message description including the missing identifier
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
