package com.pagatu.coffee.exception;

/**
 * Thrown when an authenticated user is not authorized to perform an action.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}