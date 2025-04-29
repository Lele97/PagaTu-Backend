package com.pagatu.gateway_service.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${caffe.service.url}")
    private String caffeServiceUrl;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth")))
                        .uri(authServiceUrl))
                .route("caffe-service", r -> r.path("/api/caffe/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("caffeCircuitBreaker")
                                .setFallbackUri("forward:/fallback/caffe")))
                        .uri(caffeServiceUrl))
                .route("email-service", r -> r.path("/api/email/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("emailCircuitBreaker")
                                .setFallbackUri("forward:/fallback/email")))
                        .uri(emailServiceUrl))
                .build();
    }
}
