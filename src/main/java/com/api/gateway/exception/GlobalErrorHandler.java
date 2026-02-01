package com.api.gateway.exception;


import com.api.gateway.entity.ErrorResponse;

@org.springframework.stereotype.Component
public class GlobalErrorHandler implements org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public GlobalErrorHandler(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public reactor.core.publisher.Mono<Void> handle(
            org.springframework.web.server.ServerWebExchange exchange,
            Throwable ex) {

        org.springframework.http.server.reactive.ServerHttpResponse response = exchange.getResponse();
        org.springframework.http.server.reactive.ServerHttpRequest request = exchange.getRequest();

        ErrorResponse errorResponse;
        org.springframework.http.HttpStatus status;

        if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            status = org.springframework.http.HttpStatus.NOT_FOUND;
            errorResponse = ErrorResponse.builder()
                    .error("Service Not Found")
                    .message("The requested service is not available")
                    .status(status.value())
                    .path(request.getPath().toString())
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .traceId(java.util.UUID.randomUUID().toString())
                    .build();
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            status = org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
            errorResponse = ErrorResponse.builder()
                    .error("Gateway Timeout")
                    .message("The service took too long to respond")
                    .status(status.value())
                    .path(request.getPath().toString())
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .traceId(java.util.UUID.randomUUID().toString())
                    .build();
        } else {
            status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse = ErrorResponse.builder()
                    .error("Internal Server Error")
                    .message("An unexpected error occurred")
                    .status(status.value())
                    .path(request.getPath().toString())
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .traceId(java.util.UUID.randomUUID().toString())
                    .build();
        }

        logger.error("Gateway error for path {}: {}", request.getPath(), ex.getMessage(), ex);

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            org.springframework.core.io.buffer.DataBuffer buffer =
                    response.bufferFactory().wrap(jsonResponse.getBytes());
            return response.writeWith(reactor.core.publisher.Mono.just(buffer));
        } catch (Exception e) {
            logger.error("Error writing error response", e);
            return response.setComplete();
        }
    }
}