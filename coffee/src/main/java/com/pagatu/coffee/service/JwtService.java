package com.pagatu.coffee.service;

import com.pagatu.coffee.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token processing and authentication header handling.
 * <p>
 * This service provides utilities for extracting user information from
 * JWT tokens contained in HTTP Authorization headers. It handles the
 * Bearer token format and delegates token parsing to the JwtUtil class.
 * </p>
 * <p>
 * The service validates Authorization header format and extracts tokens
 * for further processing, ensuring consistent handling of authentication
 * across all controllers.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {
    

    private final JwtUtil jwtUtil;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    /**
     * Extracts user ID from Authorization header
     * @param authHeader Authorization header value
     * @return User ID from JWT token
     * @throws IllegalArgumentException if header is invalid
     */
    public Long extractUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * Extracts username from Authorization header
     * @param authHeader Authorization header value
     * @return Username from JWT token
     * @throws IllegalArgumentException if header is invalid
     */
    public String extractUsernameFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        return jwtUtil.getUsernameFromToken(token);
    }
}
