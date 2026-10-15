package com.pagatu.auth.service;

import com.pagatu.auth.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    public Long extractUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        return jwtUtil.getUserIdFromToken(token);
    }

    public String extractUsernameFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        return jwtUtil.getUsernameFromToken(token);
    }
}