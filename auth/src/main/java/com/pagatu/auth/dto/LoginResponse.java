package com.pagatu.auth.dto;

/**
 * Record for login response containing JWT token and user information.
 * Java 17 record providing immutability and built-in equals, hashCode, toString.
 */
public record LoginResponse(
        String token,
        String username,
        String email
) {}
