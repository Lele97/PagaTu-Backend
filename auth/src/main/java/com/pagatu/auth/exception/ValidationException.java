package com.pagatu.auth.exception;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ValidationException extends RuntimeException {

    private final Map<String, List<String>> fieldErrors;

    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}
