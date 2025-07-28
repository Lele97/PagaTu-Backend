package com.pagatu.coffee.exception;

public class NoContentAvailableException extends RuntimeException {

    public NoContentAvailableException(String message) {
        super(message);
    }

    public NoContentAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
