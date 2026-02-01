package com.api.gateway.modelConfig;

import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.web.cors.reactive.CorsUtils;
import reactor.core.publisher.Mono;

@Configuration
@EnableScheduling
public class AdvancedGatewayConfig {

    @Autowired
    private CustomFilterFactory customFilterFactory;

    @Autowired
    private GatewayMetrics gatewayMetrics;

    // Global filter para métricas
    @Bean
    public GlobalFilter globalMetricsFilter() {
        return (exchange, chain) -> {
            String serviceName = exchange.getRequest().getHeaders().getFirst("X-Service-Name");
            if (serviceName != null) {
                Timer.Sample sample = gatewayMetrics.startTimer(serviceName);

                return chain.filter(exchange).doFinally(signalType -> {
                    int statusCode = exchange.getResponse().getStatusCode().value();
                    gatewayMetrics.recordResponseTime(sample, serviceName, statusCode);
                });
            }
            return chain.filter(exchange);
        };
    }

    // Global filter para CORS
    @Bean
    public GlobalFilter corsFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (CorsUtils.isPreFlightRequest(request)) {
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "*");
                headers.add("Access-Control-Max-Age", "3600");
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
            return chain.filter(exchange);
        };
    }
}
