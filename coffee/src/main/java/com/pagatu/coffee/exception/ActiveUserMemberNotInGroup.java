package com.pagatu.coffee.exception;

public class ActiveUserMemberNotInGroup extends RuntimeException {

    public ActiveUserMemberNotInGroup(String message){
        super(message);
    }
}
