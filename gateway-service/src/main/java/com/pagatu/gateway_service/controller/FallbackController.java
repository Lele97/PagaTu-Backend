package com.pagatu.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller handling fallback responses for circuit breaker patterns.
 * Provides graceful degradation when microservices are unavailable.
 * Supports all HTTP methods to prevent 405 errors.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    public static final String KEY_ERROR = "error";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_STATUS = "status";

    /**
     * Generic fallback endpoint for unspecified services.
     * Supports all HTTP methods.
     *
     * @return ResponseEntity with service unavailable status and error message
     */
    @RequestMapping(value = "/default", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Service Temporarily Unavailable");
        response.put(KEY_MESSAGE, "The requested service is currently unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for authentication service failures.
     * Supports all HTTP methods.
     *
     * @return ResponseEntity with service unavailable status and authentication-specific error message
     */
    @RequestMapping(value = "/auth", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> authFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Authentication Service Unavailable");
        response.put(KEY_MESSAGE, "Authentication service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for coffee service failures.
     * Supports all HTTP methods.
     *
     * @return ResponseEntity with service unavailable status and coffee-specific error message
     */
    @RequestMapping(value = "/coffee", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> coffeeFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Coffee Service Unavailable");
        response.put(KEY_MESSAGE, "Coffee service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback endpoint for email service failures.
     * Supports all HTTP methods.
     * NOTE: Endpoint is /email (not /mail) to match GatewayConfig.
     *
     * @return ResponseEntity with service unavailable status and email-specific error message
     */
    @RequestMapping(value = "/mail", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> emailFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_ERROR, "Email Service Unavailable");
        response.put(KEY_MESSAGE, "Email service is temporarily unavailable. Please try again later.");
        response.put(KEY_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}