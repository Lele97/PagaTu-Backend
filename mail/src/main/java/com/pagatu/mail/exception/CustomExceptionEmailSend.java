package com.pagatu.mail.exception;

/**
 * Custom exception thrown when email sending operations fail.
 * This exception extends RuntimeException and is used to wrap lower-level exceptions
 * that might occur during email transmission.
 */
public class CustomExceptionEmailSend extends RuntimeException {

    /**
     * Constructs a new CustomExceptionEmailSend with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public CustomExceptionEmailSend(String message) {
        super(message);
    }
}