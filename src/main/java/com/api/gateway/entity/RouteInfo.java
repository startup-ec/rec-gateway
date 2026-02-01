package com.api.gateway.entity;


@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class RouteInfo {

    private String routeId;
    private String path;
    private String uri;
    private String method;
    private java.time.LocalDateTime createdAt;
    private boolean active;
    private java.util.Map<String, String> filters;
    private java.util.Map<String, String> metadata;

    @lombok.Builder.Default
    private Integer priority = 0;

    @lombok.Builder.Default
    private Long timeout = 30000L; // 30 segundos
}