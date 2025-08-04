package com.pagatu.auth.controller;

import com.pagatu.auth.dto.ResetPasswordRequest;
import com.pagatu.auth.dto.LoginRequest;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.dto.TokenValidationResponse;
import com.pagatu.auth.entity.RateLimiterResult;
import com.pagatu.auth.service.AuthService;
import com.pagatu.auth.service.ProfileAwareAuthService;
import com.pagatu.auth.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final ProfileAwareAuthService profileAwareAuthService;
    //private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    public AuthController(ProfileAwareAuthService profileAwareAuthService, RateLimiterService rateLimiterService) {
        this.profileAwareAuthService = profileAwareAuthService;
        //this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = profileAwareAuthService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        profileAwareAuthService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<String> sendEmailForPasswordChange(
            @RequestParam("email") String email,
            HttpServletRequest request) {

        // Apply rate limiting based on IP address
        String clientIp = getClientIpAddress(request);
        RateLimiterResult rateLimitResult = rateLimiterService.checkRateLimit(clientIp);

        if (!rateLimitResult.isAllowed()) {
            log.warn("Rate limit exceeded for IP: {}, wait time: {} seconds",
                    clientIp, rateLimitResult.getWaitTimeSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(rateLimitResult.getWaitTimeSeconds()))
                    .body("Too many requests. Please try again later.");
        }

        profileAwareAuthService.sendEmailForResetPassword(email);
        return ResponseEntity.ok("Password reset email sent successfully");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<TokenValidationResponse> validateResetTokenFromEmail(@RequestParam("key") String token) {

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TokenValidationResponse(false, null, "Token is required"));
        }
        // Validate the token and get associated email
        String email = profileAwareAuthService.validateResetTokenAndGetEmail(token);

        if (email != null) {
            // Return success response with email for the frontend
            return ResponseEntity.ok()
                    .body(new TokenValidationResponse(true, email, "Token is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(false, null, "Invalid or expired token"));
        }
    }

    @PutMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
            @RequestHeader("X-Reset-Token") String token) {

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Reset token is required");
        }

        profileAwareAuthService.resetPassword(resetPasswordRequest, token);
        return ResponseEntity.ok("Password reset successfully");
    }

    /**
     * Extract client IP address from request, considering proxy headers
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
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}