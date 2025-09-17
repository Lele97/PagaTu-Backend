package com.pagatu.coffee.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for JWT (JSON Web Token) operations in the Coffee application.
 * This class provides methods to parse and extract information from JWT tokens,
 * including user identification and claims validation. It uses the JJWT library
 * for secure token parsing and validation.
 *
 * <p>The class is configured as a Spring component and uses a secret key
 * from application properties for token verification. All methods in this
 * class assume the tokens are valid and properly signed.</p>
 *
 * <p><strong>Security Note:</strong> The secret key used for token validation
 * should be kept secure and rotated regularly in production environments.</p>
 */
@Component
public class JwtUtil {

    /**
     * The secret key used for JWT token signing and verification.
     * This value is injected from the application properties file
     * using the key "jwt.secret".
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Extracts and returns all claims from a JWT token.
     * This method parses the provided JWT token using the configured secret key
     * and returns the claims body containing all the token's payload information.
     *
     * <p>The method creates an HMAC SHA key from the secret and uses it to
     * validate the token signature before extracting claims.</p>
     *
     * @param token the JWT token string to parse (should not include "Bearer " prefix)
     * @return Claims object containing all the token's payload data
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     * @throws io.jsonwebtoken.security.SecurityException if the token signature is invalid
     * @see Claims
     */
    public Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     * This method retrieves the "sub" (subject) claim from the token,
     * which typically contains the username or user identifier.
     *
     * <p>The method automatically handles tokens with the "Bearer " prefix
     * by stripping it before processing. This is common when tokens are
     * received from HTTP Authorization headers.</p>
     *
     * @param token the JWT token string, may include "Bearer " prefix
     * @return the username/subject stored in the token's "sub" claim
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     * @throws io.jsonwebtoken.security.SecurityException if the token signature is invalid
     * @see #getAllClaimsFromToken(String)
     */
    public String getUsernameFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Extracts the user ID from a JWT token.
     * This method retrieves the custom "id" claim from the token and converts
     * it to a Long value. This claim typically contains the unique user identifier
     * from the database.
     *
     * <p>The method automatically handles tokens with the "Bearer " prefix
     * by stripping it before processing. The "id" claim is expected to be
     * present and convertible to a Long value.</p>
     *
     * @param token the JWT token string, may include "Bearer " prefix
     * @return the user ID as a Long value extracted from the "id" claim
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     * @throws io.jsonwebtoken.security.SecurityException if the token signature is invalid
     * @throws NumberFormatException if the "id" claim cannot be converted to Long
     * @throws NullPointerException if the "id" claim is not present in the token
     * @see #getAllClaimsFromToken(String)
     */
    public Long getUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Long.valueOf(getAllClaimsFromToken(token).get("id").toString());
    }
}