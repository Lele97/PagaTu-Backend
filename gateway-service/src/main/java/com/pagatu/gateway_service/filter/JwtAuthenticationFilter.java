package com.pagatu.gateway_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/reset-password",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/auth/forgotPassword",
            "/actuator",
            "/fallback"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        HttpMethod method = request.getMethod();
        log.debug("Processing request: {} {}", method, path);

        // Handle preflight requests
        if (method == HttpMethod.OPTIONS) {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }
        // Skip authentication for open endpoints
        if (isOpenApiRequest(path)) {
            log.debug("Skipping authentication for: {}", path);
            return chain.filter(exchange);
        }

        // Check for Authorization header
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.debug("Missing Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Authorization Required");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Invalid Authorization header format for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Authorization header format");
        }

        String token = authHeader.substring(7);
        if (!isValidToken(token)) {
            log.debug("Invalid JWT token for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }

        log.debug("Valid JWT token for path: {}", path);
        return chain.filter(exchange);
    }

    private boolean isOpenApiRequest(String path) {
        return openApiEndpoints.stream().anyMatch(path::startsWith);
    }

    private boolean isValidToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.debug("Token validation successful for user: {}", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(status);
        response.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                status.getReasonPhrase(), message);

        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Override
    public int getOrder() {
        return 1; // Run after CORS filter (which has order 0)
    }
}