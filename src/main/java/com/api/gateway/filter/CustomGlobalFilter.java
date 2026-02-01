package com.api.gateway.filter;


@org.springframework.stereotype.Component
public class CustomGlobalFilter implements org.springframework.cloud.gateway.filter.GlobalFilter,
        org.springframework.core.Ordered {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(CustomGlobalFilter.class);

    @Override
    public reactor.core.publisher.Mono<Void> filter(
            org.springframework.web.server.ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        // Pre-processing: Logging de request
        org.springframework.http.server.reactive.ServerHttpRequest request = exchange.getRequest();
        logger.info("Gateway processing request: {} {}",
                request.getMethod(), request.getURI());

        // Agregar timestamp al request
        String timestamp = String.valueOf(System.currentTimeMillis());
        org.springframework.http.server.reactive.ServerHttpRequest modifiedRequest =
                request.mutate()
                        .header("X-Timestamp", timestamp)
                        .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(reactor.core.publisher.Mono.fromRunnable(() -> {
                    // Post-processing: Logging de response
                    org.springframework.http.server.reactive.ServerHttpResponse response =
                            exchange.getResponse();
                    logger.info("Gateway processed request with status: {}",
                            response.getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        return -1; // Ejecutar antes que otros filtros
    }
}