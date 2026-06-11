package com.pagatu.coffee.exception;

/**
 * Exception thrown when a query returns no data (mapped to HTTP 204).
 */
public class NoContentAvailableException extends RuntimeException {

    /**
     * @param message description of the empty result set
     */
    public NoContentAvailableException(String message) {
        super(message);
    }
}
