package com.pagatu.coffee.exception;

/**
 * Exception thrown when a user has no group memberships.
 */
public class UserNotInGroup extends RuntimeException {

    /**
     * @param message description of the missing membership condition
     */
    public UserNotInGroup(String message) {
        super(message);
    }
}
