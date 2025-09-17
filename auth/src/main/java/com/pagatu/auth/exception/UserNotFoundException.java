package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class UserNotFoundException extends RuntimeException{

    private final String identifier;
    private final String identifierType;

    public UserNotFoundException(String message, String identifier, String identifierType) {
        super(message);
        this.identifier = identifier;
        this.identifierType = identifierType;
    }
}
