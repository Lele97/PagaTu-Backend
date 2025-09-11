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

/**
 * Global filter for JWT authentication in the API Gateway.
 * Validates JWT tokens for protected endpoints and handles authentication errors.
 * Implements Ordered interface to control filter execution order.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * List of endpoints that don't require authentication.
     * Includes auth endpoints, Swagger, API docs, and actuator endpoints.
     */
    private final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/reset-password",
            "/api/auth/resetPassword",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/auth/forgotPassword",
            "/actuator",
            "/fallback"
    );

    /**
     * Filters incoming requests to validate JWT tokens for protected endpoints.
     *
     * @param exchange ServerWebExchange containing the request and response
     * @param chain GatewayFilterChain for continuing filter processing
     * @return Mono<Void> indicating completion of filter processing
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        HttpMethod method = request.getMethod();

        log.debug("Processing request: {} {}", method, path);

        if (method == HttpMethod.OPTIONS) {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        if (isOpenApiRequest(path)) {
            log.debug("Skipping authentication for: {}", path);
            return chain.filter(exchange);
        }

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

    /**
     * Checks if the requested path is in the open endpoints list.
     *
     * @param path Request path to check
     * @return boolean true if path doesn't require authentication
     */
    private boolean isOpenApiRequest(String path) {
        return openApiEndpoints.stream().anyMatch(path::startsWith);
    }

    /**
     * Validates the JWT token using the configured secret key.
     *
     * @param token JWT token to validate
     * @return boolean true if token is valid
     */
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

    /**
     * Handles authentication errors by returning appropriate HTTP status and message.
     *
     * @param exchange ServerWebExchange containing the request and response
     * @param status HTTP status code to return
     * @param message Error message to return
     * @return Mono<Void> with error response
     */
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

    /**
     * Gets the order value for this filter.
     * Lower values have higher priority. Value 1 ensures this runs after CORS filter.
     *
     * @return int order value
     */
    @Override
    public int getOrder() {
        return 1;
    }
}