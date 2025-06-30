package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class UserAlreadyExistsException extends RuntimeException{
    private final String field;
    private final String value;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public UserAlreadyExistsException(String message, String field, String value) {
        super(message);
        this.field = field;
        this.value = value;
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.value = null;
    }
}
