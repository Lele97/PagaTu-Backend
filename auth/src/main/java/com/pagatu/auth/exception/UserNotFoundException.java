package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class UserNotFoundException extends RuntimeException{
    private final String identifier;
    private final String identifierType;

    public UserNotFoundException(String message) {
        super(message);
        this.identifier = null;
        this.identifierType = null;
    }

    public UserNotFoundException(String message, String identifier, String identifierType) {
        super(message);
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.identifier = null;
        this.identifierType = null;
    }
}
