package com.pagatu.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class FallbackController {
    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, String>>> authServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Il servizio di autenticazione non è disponibile al momento. Riprova più tardi.");
        response.put("service", "Auth Service");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/caffe")
    public Mono<ResponseEntity<Map<String, String>>> caffeServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Il servizio caffè non è disponibile al momento. Riprova più tardi.");
        response.put("service", "Caffè Service");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/email")
    public Mono<ResponseEntity<Map<String, String>>> emailServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Il servizio email non è disponibile al momento. Riprova più tardi.");
        response.put("service", "Email Service");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/default")
    public Mono<ResponseEntity<Map<String, String>>> defaultFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Il servizio richiesto non è disponibile al momento. Riprova più tardi.");
        response.put("service", "Unknown Service");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
