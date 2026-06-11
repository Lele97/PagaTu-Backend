package com.pagatu.auth.exception;

import lombok.Data;

/**
 * Exception thrown when registering a user with an existing username or email.
 */
@Data
public class UserAlreadyExistsException extends RuntimeException{

    private final String field;
    private final String value;

    /**
     * @param message error description
     * @param field   conflicting field name
     * @param value   conflicting field value
     */
    public UserAlreadyExistsException(String message, String field, String value) {
        super(message);
        this.field = field;
        this.value = value;
    }
}
