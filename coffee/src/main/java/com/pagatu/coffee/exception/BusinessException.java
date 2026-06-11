package com.pagatu.coffee.exception;

/**
 * Exception thrown when a business rule is violated in the coffee domain.
 * Mapped to HTTP 400 by {@link com.pagatu.coffee.exception.GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    /**
     * @param message human-readable description of the violated rule
     */
    public BusinessException(String message) {
        super(message);
    }
}
