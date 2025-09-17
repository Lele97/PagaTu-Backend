package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class UserAlreadyExistsException extends RuntimeException{

    private final String field;
    private final String value;

    public UserAlreadyExistsException(String message, String field, String value) {
        super(message);
        this.field = field;
        this.value = value;
    }
}
