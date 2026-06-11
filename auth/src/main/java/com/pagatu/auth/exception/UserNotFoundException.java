package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when a requested user cannot be found.
 */
@Data
public class UserNotFoundException extends RuntimeException{

    private final String identifier;
    private final String identifierType;

    /**
     * @param message         error description
     * @param identifier      value that was searched (username, email, etc.)
     * @param identifierType  type of identifier (e.g. username, email)
     */
    public UserNotFoundException(String message, String identifier, String identifierType) {
        super(message);
        this.identifier = identifier;
        this.identifierType = identifierType;
    }
}
