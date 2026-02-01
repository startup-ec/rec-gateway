package com.api.gateway.modelConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.stereotype.Component;

@Component
public class CustomFilterFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomFilterFactory.class);

    public GatewayFilter createRateLimitFilter(String serviceName, int requestsPerSecond) {
        return (exchange, chain) -> {
            // Implementar rate limiting básico
            String clientId = exchange.getRequest().getRemoteAddress().toString();
            logger.debug("Rate limiting check for service {} and client {}", serviceName, clientId);
            return chain.filter(exchange);
        };
    }

    public GatewayFilter createLoggingFilter(String serviceName) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequest().getPath().value();

            logger.info("Request to {} service: {} {}", serviceName,
                    exchange.getRequest().getMethod(), path);

            return chain.filter(exchange).doFinally(signalType -> {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Response from {} service: {} ms", serviceName, duration);
            });
        };
    }
}