package com.api.gateway.controller;

import com.api.gateway.entity.RouteInfo;
import com.api.gateway.entity.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/gateway")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final org.springframework.cloud.gateway.route.RouteLocator routeLocator;

    public GatewayController(org.springframework.cloud.gateway.route.RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
        logger.info("GatewayController initialized successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/routes")
    public reactor.core.publisher.Flux<RouteInfo> getRoutes() {
        logger.info("Fetching all gateway routes");

        return routeLocator.getRoutes()
                .doOnSubscribe(subscription -> logger.debug("Starting route retrieval"))
                .map(route -> {
                    logger.debug("Processing route: {}", route.getId());
                    return RouteInfo.builder()
                            .routeId(route.getId())
                            .path(extractPath(route))
                            .uri(route.getUri().toString())
                            .method("ALL")
                            .createdAt(java.time.LocalDateTime.now())
                            .active(true)
                            .priority(route.getOrder())
                            .filters(extractFilters(route))
                            .metadata(extractMetadata(route))
                            .build();
                })
                .doOnComplete(() -> logger.info("Successfully retrieved all routes"))
                .doOnError(error -> logger.error("Error retrieving routes", error));
    }

    @org.springframework.web.bind.annotation.GetMapping("/info")
    public reactor.core.publisher.Mono<java.util.Map<String, Object>> getGatewayInfo() {
        logger.info("Fetching gateway information");

        try {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("name", "API Gateway");
            info.put("version", "1.0.0");
            info.put("status", "UP");
            info.put("timestamp", java.time.LocalDateTime.now());

            String port = getServerPort();
            info.put("port", port);
            logger.debug("Gateway running on port: {}", port);

            info.put("configured-services", java.util.Arrays.asList(
                    "notification-service",
                    "rec-cognito",
                    "rec-leaf-scan-cacao"
            ));

            Long totalRoutes = routeLocator.getRoutes().count().block();
            info.put("total-routes", totalRoutes);
            logger.debug("Total configured routes: {}", totalRoutes);

            logger.info("Gateway information retrieved successfully");
            return reactor.core.publisher.Mono.just(info);

        } catch (Exception e) {
            logger.error("Error retrieving gateway information", e);
            throw e;
        }
    }

    private String getServerPort() {
        try {
            String port = System.getProperty("server.port", "8080");
            logger.debug("Retrieved server port: {}", port);
            return port;
        } catch (Exception e) {
            logger.warn("Error retrieving server port, using default 8080", e);
            return "8080";
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/services")
    public reactor.core.publisher.Flux<ServiceInfo> getServices() {
        logger.info("Fetching all configured services");

        return reactor.core.publisher.Flux.fromIterable(getConfiguredServices())
                .doOnSubscribe(subscription -> logger.debug("Starting services retrieval"))
                .doOnNext(service -> logger.debug("Retrieved service: {}", service.getServiceName()))
                .doOnComplete(() -> logger.info("Successfully retrieved all services"))
                .doOnError(error -> logger.error("Error retrieving services", error));
    }

    @org.springframework.web.bind.annotation.GetMapping("/services/{serviceName}/health")
    public reactor.core.publisher.Mono<ServiceInfo> checkServiceHealth(
            @org.springframework.web.bind.annotation.PathVariable String serviceName) {

        logger.info("Checking health for service: {}", serviceName);

        String serviceUrl = getServiceUrl(serviceName);
        if (serviceUrl == null) {
            logger.warn("Service not found: {}", serviceName);
            return reactor.core.publisher.Mono.just(
                    ServiceInfo.builder()
                            .serviceName(serviceName)
                            .status("NOT_FOUND")
                            .healthy(false)
                            .lastCheck(java.time.LocalDateTime.now())
                            .build()
            );
        }

        logger.debug("Service URL found for {}: {}", serviceName, serviceUrl);
        return checkServiceStatus(serviceName, serviceUrl)
                .doOnSuccess(serviceInfo ->
                        logger.info("Health check completed for {}: status={}, healthy={}",
                                serviceName, serviceInfo.getStatus(), serviceInfo.isHealthy()))
                .doOnError(error ->
                        logger.error("Error checking health for service: {}", serviceName, error));
    }

    @org.springframework.web.bind.annotation.GetMapping("/services/status")
    public reactor.core.publisher.Mono<java.util.Map<String, Object>> getAllServicesStatus() {
        logger.info("Fetching status for all services");

        try {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("gateway", "UP");
            status.put("timestamp", java.time.LocalDateTime.now());
            status.put("services", getConfiguredServices());

            logger.info("Successfully retrieved status for all services");
            return reactor.core.publisher.Mono.just(status);

        } catch (Exception e) {
            logger.error("Error retrieving services status", e);
            throw e;
        }
    }

    private String extractPath(org.springframework.cloud.gateway.route.Route route) {
        try {
            String path = route.getPredicate().toString().contains("Paths")
                    ? route.getPredicate().toString()
                    : "/unknown";
            logger.debug("Extracted path for route {}: {}", route.getId(), path);
            return path;
        } catch (Exception e) {
            logger.warn("Error extracting path for route {}", route.getId(), e);
            return "/unknown";
        }
    }

    private java.util.Map<String, String> extractFilters(org.springframework.cloud.gateway.route.Route route) {
        java.util.Map<String, String> filters = new java.util.HashMap<>();
        try {
            route.getFilters().forEach(filter -> {
                String filterName = filter.getClass().getSimpleName();
                filters.put(filterName, filter.toString());
                logger.debug("Extracted filter for route {}: {}", route.getId(), filterName);
            });
        } catch (Exception e) {
            logger.warn("Error extracting filters for route {}", route.getId(), e);
        }
        return filters;
    }

    private java.util.Map<String, String> extractMetadata(org.springframework.cloud.gateway.route.Route route) {
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        try {
            route.getMetadata().forEach((key, value) -> {
                metadata.put(key.toString(), value != null ? value.toString() : "null");
                logger.debug("Extracted metadata for route {}: {}={}", route.getId(), key, value);
            });
        } catch (Exception e) {
            logger.warn("Error extracting metadata for route {}", route.getId(), e);
        }
        return metadata;
    }

    private java.util.List<ServiceInfo> getConfiguredServices() {
        logger.debug("Building configured services list");
        java.util.List<ServiceInfo> services = new java.util.ArrayList<>();

        try {
            // Notification Service
            java.util.Map<String, String> notificationEndpoints = new java.util.HashMap<>();
            // Email endpoints
            notificationEndpoints.put("send-email", "/api/notifications/email");
            notificationEndpoints.put("send-email-attachments", "/api/notifications/email/with-attachments");
            // Push notification endpoints
            notificationEndpoints.put("send-push", "/api/notifications/push");
            notificationEndpoints.put("register-fcm", "/api/notifications/fcm/register");
            notificationEndpoints.put("unregister-fcm", "/api/notifications/fcm/unregister/{userId}");
            // Query endpoints
            notificationEndpoints.put("history", "/api/notifications/history/{recipient}");
            notificationEndpoints.put("by-status", "/api/notifications/status/{status}");
            notificationEndpoints.put("by-id", "/api/notifications/{id}");
            notificationEndpoints.put("date-range", "/api/notifications/date-range");
            // Health endpoints
            notificationEndpoints.put("health", "/notifications/health");
            notificationEndpoints.put("info", "/notifications/info");
            notificationEndpoints.put("status", "/notifications/status");
            notificationEndpoints.put("stats", "/notifications/stats");
            notificationEndpoints.put("ping", "/ping");

            services.add(ServiceInfo.builder()
                    .serviceName("notification-service")
                    .serviceUrl("http://localhost:8081")
                    .port(8081)
                    .status("UNKNOWN")
                    .endpoints(notificationEndpoints)
                    .lastCheck(java.time.LocalDateTime.now())
                    .build());

            logger.debug("Added notification-service with {} endpoints", notificationEndpoints.size());

            // Rec-Cognito Service
            java.util.Map<String, String> cognitoEndpoints = new java.util.HashMap<>();
            // Auth endpoints
            cognitoEndpoints.put("signup", "/api/v1/auth/signup");
            cognitoEndpoints.put("signin", "/api/v1/auth/signin");
            cognitoEndpoints.put("confirm-signup", "/api/v1/auth/confirm-signup");
            cognitoEndpoints.put("refresh-token", "/api/v1/auth/refresh-token");
            cognitoEndpoints.put("change-password", "/api/v1/auth/change-password");
            cognitoEndpoints.put("forgot-password", "/api/v1/auth/forgot-password");
            cognitoEndpoints.put("confirm-forgot-password", "/api/v1/auth/confirm-forgot-password");
            // Admin endpoints
            cognitoEndpoints.put("get-users", "/api/v1/admin/users");
            cognitoEndpoints.put("get-roles", "/api/v1/admin/roles");
            cognitoEndpoints.put("create-role", "/api/v1/admin/roles");
            cognitoEndpoints.put("assign-role", "/api/v1/admin/users/{userId}/roles/{roleId}");
            cognitoEndpoints.put("remove-role", "/api/v1/admin/users/{userId}/roles/{roleId}");
            // Health endpoints
            cognitoEndpoints.put("health", "/cognito/health");
            cognitoEndpoints.put("info", "/cognito/info");
            cognitoEndpoints.put("status", "/cognito/status");
            cognitoEndpoints.put("ping", "/ping");

            services.add(ServiceInfo.builder()
                    .serviceName("rec-cognito")
                    .serviceUrl("http://localhost:8082")
                    .port(8082)
                    .status("UNKNOWN")
                    .endpoints(cognitoEndpoints)
                    .lastCheck(java.time.LocalDateTime.now())
                    .build());

            logger.debug("Added rec-cognito service with {} endpoints", cognitoEndpoints.size());

            // Rec-Leaf-Scan-Cacao Service
            java.util.Map<String, String> leafScanEndpoints = new java.util.HashMap<>();
            // Cultivos CRUD endpoints
            leafScanEndpoints.put("create-cultivo", "/api/v1/cultivos");
            leafScanEndpoints.put("get-cultivo-by-id", "/api/v1/cultivos/{id}");
            leafScanEndpoints.put("get-all-cultivos", "/api/v1/cultivos");
            leafScanEndpoints.put("update-cultivo", "/api/v1/cultivos/{id}");
            leafScanEndpoints.put("delete-cultivo", "/api/v1/cultivos/{id}");
            // Query endpoints
            leafScanEndpoints.put("cultivos-by-usuario", "/api/v1/cultivos/usuario/{usuarioId}");
            leafScanEndpoints.put("cultivos-by-estado", "/api/v1/cultivos/estado/{estadoCultivo}");
            leafScanEndpoints.put("cultivos-by-usuario-estado", "/api/v1/cultivos/usuario/{usuarioId}/estado/{estadoCultivo}");
            leafScanEndpoints.put("cultivos-by-variedad", "/api/v1/cultivos/variedad/{variedadCacao}");
            leafScanEndpoints.put("area-total-activa", "/api/v1/cultivos/usuario/{usuarioId}/area-total-activa");
            leafScanEndpoints.put("verify-existence", "/api/v1/cultivos/{id}/exists");
            leafScanEndpoints.put("change-estado", "/api/v1/cultivos/{id}/estado");
            // Health endpoints
            leafScanEndpoints.put("health", "/leaf-scan/health");
            leafScanEndpoints.put("info", "/leaf-scan/info");
            leafScanEndpoints.put("status", "/leaf-scan/status");
            leafScanEndpoints.put("ping", "/ping");

            services.add(ServiceInfo.builder()
                    .serviceName("rec-leaf-scan-cacao")
                    .serviceUrl("http://localhost:8083")
                    .port(8083)
                    .status("UNKNOWN")
                    .endpoints(leafScanEndpoints)
                    .lastCheck(java.time.LocalDateTime.now())
                    .build());

            logger.debug("Added rec-leaf-scan-cacao service with {} endpoints", leafScanEndpoints.size());
            logger.info("Successfully configured {} services", services.size());

        } catch (Exception e) {
            logger.error("Error building configured services list", e);
            throw e;
        }

        return services;
    }

    private String getServiceUrl(String serviceName) {
        logger.debug("Looking up URL for service: {}", serviceName);

        String url = null;
        switch (serviceName.toLowerCase()) {
            case "notification-service":
            case "notifications":
                url = "http://localhost:8081";
                break;
            case "rec-cognito":
            case "cognito":
                url = "http://localhost:8082";
                break;
            case "rec-leaf-scan-cacao":
            case "leaf-scan":
            case "leaf-scan-cacao":
                url = "http://localhost:8083";
                break;
            default:
                logger.warn("Unknown service name: {}", serviceName);
                return null;
        }

        logger.debug("Found URL for service {}: {}", serviceName, url);
        return url;
    }

    private reactor.core.publisher.Mono<ServiceInfo> checkServiceStatus(
            String serviceName, String serviceUrl) {

        logger.debug("Performing health check for service: {} at URL: {}", serviceName, serviceUrl);
        long startTime = System.currentTimeMillis();

        // Simular verificación de salud (en un caso real harías una llamada HTTP)
        return reactor.core.publisher.Mono.just(
                ServiceInfo.builder()
                        .serviceName(serviceName)
                        .serviceUrl(serviceUrl)
                        .status("UP") // En producción, verificarías realmente el endpoint
                        .healthy(true)
                        .responseTime(System.currentTimeMillis() - startTime)
                        .lastCheck(java.time.LocalDateTime.now())
                        .version("1.0.0")
                        .build()
        ).doOnSuccess(result ->
                logger.info("Health check successful for {}: responseTime={}ms",
                        serviceName, result.getResponseTime())
        ).doOnError(error ->
                logger.error("Health check failed for service: {}", serviceName, error)
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("/health")
    public reactor.core.publisher.Mono<java.util.Map<String, String>> health() {
        logger.debug("Gateway health check requested");

        try {
            java.util.Map<String, String> health = new java.util.HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", java.time.LocalDateTime.now().toString());

            logger.debug("Gateway health check: status=UP");
            return reactor.core.publisher.Mono.just(health);

        } catch (Exception e) {
            logger.error("Error during gateway health check", e);
            throw e;
        }
    }
}*/
