package com.pagatu.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Record for login request containing username and password.
 * Java 17 record providing immutability and built-in equals, hashCode, toString.
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,
        
        @NotBlank(message = "Password is required")
        String password
) {}

