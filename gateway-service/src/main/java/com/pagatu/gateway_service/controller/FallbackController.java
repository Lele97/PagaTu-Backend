package com.pagatu.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller handling fallback responses for circuit breaker patterns.
 * Provides graceful degradation when microservices are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    public static final String KEY_ERROR = "error";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_STATUS = "status";

    /**
     * Generic fallback endpoint for unspecified services.
     *
     * @return ResponseEntity with service unavailable status and error message
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Service Temporarily Unavailable");
        response.put(KEY_MESSAGE, "The requested service is currently unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for authentication service failures.
     *
     * @return ResponseEntity with service unavailable status and authentication-specific error message
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Authentication Service Unavailable");
        response.put(KEY_MESSAGE, "Authentication service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for coffee service failures.
     *
     * @return ResponseEntity with service unavailable status and coffee-specific error message
     */
    @GetMapping("/coffee")
    public ResponseEntity<Map<String, Object>> coffeeFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Coffee Service Unavailable");
        response.put(KEY_MESSAGE, "Coffee service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for email service failures.
     *
     * @return ResponseEntity with service unavailable status and email-specific error message
     */
    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> emailFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Email Service Unavailable");
        response.put(KEY_MESSAGE, "Email service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
