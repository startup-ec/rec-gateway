package com.api.gateway.entity;


import lombok.Builder;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class ServiceInfo {

    private String serviceName;
    private String serviceUrl;
    private String status;
    private java.time.LocalDateTime lastCheck;
    private Long responseTime;
    private String version;
    private java.util.Map<String, String> endpoints;
    private boolean requiresAuth;

    @lombok.Builder.Default
    private Boolean healthy = false;

    @lombok.Builder.Default
    private Integer port = 8080;
    
    public boolean isHealthy() {
        return Boolean.TRUE.equals(healthy);
    }
}