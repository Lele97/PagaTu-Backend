package com.pagatu.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     *
     * @return
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Temporarily Unavailable");
        response.put("message", "The requested service is currently unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     *
     * @return
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Authentication Service Unavailable");
        response.put("message", "Authentication service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     *
     * @return
     */
    @GetMapping("/coffee")
    public ResponseEntity<Map<String, Object>> coffeeFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Coffee Service Unavailable");
        response.put("message", "Coffee service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     *
     * @return
     */
    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> emailFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Email Service Unavailable");
        response.put("message", "Email service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
