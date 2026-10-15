package com.pagatu.coffee.exception;

/**
 * Exception thrown when a group cannot be found by name.
 */
public class GroupNotFoundException extends RuntimeException {

    /**
     * @param message description including the missing group name
     */
    public GroupNotFoundException(String message) {
        super(message);
    }
}
