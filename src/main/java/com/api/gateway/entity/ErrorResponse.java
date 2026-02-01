package com.api.gateway.entity;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class ErrorResponse {

    private String error;
    private String message;
    private String path;
    private Integer status;
    private String timestamp;
    private String traceId;

    @lombok.Builder.Default
    private String gateway = "api-gateway";
}