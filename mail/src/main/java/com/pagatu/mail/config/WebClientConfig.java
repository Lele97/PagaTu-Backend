package com.pagatu.mail.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

/**
 * Configuration class for WebClient beans.
 * <p>
 * This configuration provides the necessary WebClient.Builder bean
 * that can be used throughout the application for making HTTP requests
 * to external services.
 * </p>
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder directWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Creates and configures a smart WebClient.Builder bean.
     * Supports both load-balanced service IDs (e.g. lb://coffee or http://coffee)
     * and direct host URLs / IP addresses / FQDNs without requiring manual code edits across environments.
     *
     * @param lbFilterProvider Spring Cloud LoadBalancer exchange filter function provider
     * @return a configured WebClient.Builder instance
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> lbFilterProvider) {
        ReactorLoadBalancerExchangeFilterFunction lbFilter = lbFilterProvider.getIfAvailable();
        return WebClient.builder()
                .filter((request, next) -> {
                    URI uri = request.url();
                    String host = uri.getHost();
                    boolean isDirect = host == null
                            || host.contains(".")
                            || host.equalsIgnoreCase("localhost")
                            || host.equalsIgnoreCase("127.0.0.1")
                            || (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443);

                    if (isDirect || lbFilter == null) {
                        return next.exchange(request);
                    } else {
                        return lbFilter.filter(request, next);
                    }
                });
    }
}