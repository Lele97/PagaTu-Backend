package com.pagatu.auth.exception;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when request validation fails with field-level errors.
 */
@Data
public class ValidationException extends RuntimeException {

    private final Map<String, List<String>> fieldErrors;

    /**
     * @param message     summary error message
     * @param fieldErrors map of field names to validation error messages
     */
    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}
