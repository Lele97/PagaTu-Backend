package com.pagatu.coffee.exception;

/**
 * Exception thrown when payment rotation cannot continue because a group has no active members.
 */
public class ActiveUserMemberNotInGroup extends RuntimeException {

    /**
     * @param message description of the group membership problem
     */
    public ActiveUserMemberNotInGroup(String message){
        super(message);
    }
}
