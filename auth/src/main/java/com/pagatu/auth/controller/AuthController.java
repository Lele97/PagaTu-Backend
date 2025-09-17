package com.pagatu.auth.controller;

import com.pagatu.auth.dto.*;
import com.pagatu.auth.entity.RateLimiterResult;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.UserNotFoundException;
import com.pagatu.auth.service.AuthService;
import com.pagatu.auth.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling authentication-related operations.
 * Provides endpoints for user registration, login, password reset functionality,
 * and user information retrieval. Includes rate limiting for sensitive operations.
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    /**
     * Constructs an AuthController with the required services.
     *
     * @param authService        the authentication service for handling business logic
     * @param rateLimiterService the rate limiting service for protecting sensitive endpoints
     */
    public AuthController(AuthService authService, RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Authenticates a user and returns a JWT token upon successful authentication.
     *
     * @param loginRequest the login credentials containing username and password
     * @return ResponseEntity containing the authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user in the system.
     *
     * @param registerRequest the user registration information
     * @return ResponseEntity with a success message and HTTP 201 status
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    /**
     * Initiates a password reset process by sending an email to the user.
     * Includes rate limiting to prevent abuse of the password reset functionality.
     *
     * @param email   the email address of the user requesting password reset
     * @param request the HTTP request to extract client IP for rate limiting
     * @return ResponseEntity with success message or rate limit exceeded error
     */
    @PostMapping("/forgotPassword")
    public ResponseEntity<String> sendEmailForPasswordChange(
            @RequestParam("email") String email,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        RateLimiterResult rateLimitResult = rateLimiterService.checkRateLimit(clientIp);

        if (!rateLimitResult.isAllowed()) {
            log.warn("Rate limit exceeded for IP: {}, wait time: {} seconds",
                    clientIp, rateLimitResult.getWaitTimeSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(rateLimitResult.getWaitTimeSeconds()))
                    .body("Too many requests. Please try again later.");
        }

        authService.sendEmailForResetPassword(email);
        return ResponseEntity.ok("Password reset email sent successfully");
    }

    /**
     * Validates a password reset token extracted from an email link.
     *
     * @param token the reset token to validate
     * @return ResponseEntity with token validation result including status and user email if valid
     */
    @GetMapping("/reset-password")
    public ResponseEntity<TokenValidationResponse> validateResetTokenFromEmail(@RequestParam("key") String token) {

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TokenValidationResponse(false, null, "Token is required"));
        }

        String email = authService.validateResetTokenAndGetEmail(token);

        if (email != null) {
            return ResponseEntity.ok()
                    .body(new TokenValidationResponse(true, email, "Token is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(false, null, "Invalid or expired token"));
        }
    }

    /**
     * Resets a user's password using a valid reset token.
     *
     * @param resetPasswordRequest the new password information
     * @param token                the reset token obtained from the email link
     * @return ResponseEntity with success message or error if token is invalid
     */
    @PutMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
            @RequestHeader("X-Reset-Token") String token) {

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Reset token is required");
        }

        authService.resetPassword(resetPasswordRequest, token);
        return ResponseEntity.ok("Password reset successfully");
    }

    /**
     * Retrieves user information by email address.
     *
     * @param email the email address of the user to retrieve
     * @return ResponseEntity with user information or 404 status if user not found
     */
    @GetMapping("/user/get")
    public ResponseEntity<User> getUser(@RequestParam("email") String email) {
        try {
            User user = authService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Extracts the client IP address from the HTTP request, considering various proxy headers.
     * This method checks multiple headers commonly used by proxies and load balancers
     * to forward the original client IP address.
     *
     * @param request the HTTP request to extract the client IP from
     * @return the client IP address as a string
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}