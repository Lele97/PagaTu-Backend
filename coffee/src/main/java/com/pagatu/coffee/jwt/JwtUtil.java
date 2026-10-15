package com.pagatu.coffee.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Utility class for JWT token parsing and validation in the coffee module.
 * <p>
 * Provides methods to extract claims, user ID and username from JWT tokens.
 * Uses the same secret key as the auth module for consistent token validation.
 * </p>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Parses the token and returns all claims.
     *
     * @param token the JWT token
     * @return the claims contained in the token
     */
    public Claims getAllClaimsFromToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the username (subject) from the token.
     *
     * @param token the JWT token
     * @return the username from the token subject
     */
    public String getUsernameFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Extracts the user ID from the token claims.
     *
     * @param token the JWT token
     * @return the user ID as Long
     */
    public Long getUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Long.valueOf(getAllClaimsFromToken(token).get("id").toString());
    }
}