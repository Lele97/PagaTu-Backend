package com.pagatu.auth.exception;

import lombok.Data;

@Data
public class TokenExpiredException extends RuntimeException{

    private final String tokenType;

    public TokenExpiredException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }
}
