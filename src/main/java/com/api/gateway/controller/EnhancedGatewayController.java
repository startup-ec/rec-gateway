package com.api.gateway.controller;


import com.api.gateway.entity.RouteInfo;
import com.api.gateway.entity.ServiceInfo;
import com.api.gateway.modelConfig.GatewayServicesProperties;
import com.api.gateway.modelConfig.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gateway")
public class EnhancedGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedGatewayController.class);

    private final org.springframework.cloud.gateway.route.RouteLocator routeLocator;
    private final GatewayServicesProperties servicesProperties;
    private final WebClient webClient;

    public EnhancedGatewayController(
            org.springframework.cloud.gateway.route.RouteLocator routeLocator,
            @Autowired(required = false) GatewayServicesProperties servicesProperties) {
        this.routeLocator = routeLocator;
        this.servicesProperties = servicesProperties;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        logger.info("Enhanced GatewayController initialized successfully");
    }

    @GetMapping("/routes")
    public Flux<RouteInfo> getRoutes() {
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
                            .createdAt(LocalDateTime.now())
                            .active(true)
                            .priority(route.getOrder())
                            .filters(extractFilters(route))
                            .metadata(extractMetadata(route))
                            .build();
                })
                .doOnComplete(() -> logger.info("Successfully retrieved all routes"))
                .doOnError(error -> logger.error("Error retrieving routes", error));
    }

    @GetMapping("/info")
    public Mono<Map<String, Object>> getGatewayInfo() {
        logger.info("Fetching gateway information");

        try {
            Map<String, Object> info = new HashMap<>();
            info.put("name", "Enhanced API Gateway");
            info.put("version", "2.0.0");
            info.put("status", "UP");
            info.put("timestamp", LocalDateTime.now());
            info.put("port", getServerPort());

            // Servicios configurados dinámicamente
            if (servicesProperties != null && servicesProperties.getServices() != null) {
                info.put("configured-services", servicesProperties.getServices().keySet());
                info.put("total-configured-services", servicesProperties.getServices().size());

                // Estadísticas adicionales
                Map<String, Object> stats = new HashMap<>();
                stats.put("services-with-auth", countServicesWithAuth());
                stats.put("total-endpoints", countTotalEndpoints());
                info.put("statistics", stats);
            } else {
                // Fallback a configuración hardcoded si no hay YAML
                info.put("configured-services", Arrays.asList(
                        "notification-service", "rec-cognito", "rec-leaf-scan-cacao", "rec-gamificacion"
                ));
                info.put("configuration-mode", "legacy");
            }

            Long totalRoutes = routeLocator.getRoutes().count().block();
            info.put("total-routes", totalRoutes);

            logger.info("Gateway information retrieved successfully");
            return Mono.just(info);

        } catch (Exception e) {
            logger.error("Error retrieving gateway information", e);
            throw e;
        }
    }

    @GetMapping("/services")
    public Flux<ServiceInfo> getServices() {
        logger.info("Fetching all configured services");

        if (servicesProperties != null && servicesProperties.getServices() != null) {
            // Usar configuración YAML dinámica
            return Flux.fromIterable(buildServicesFromConfig())
                    .doOnSubscribe(subscription -> logger.debug("Starting services retrieval from YAML config"))
                    .doOnNext(service -> logger.debug("Retrieved service: {}", service.getServiceName()))
                    .doOnComplete(() -> logger.info("Successfully retrieved all services from configuration"))
                    .doOnError(error -> logger.error("Error retrieving services", error));
        } else {
            // Fallback a configuración hardcoded
            return Flux.fromIterable(getLegacyConfiguredServices())
                    .doOnSubscribe(subscription -> logger.debug("Starting services retrieval from legacy config"))
                    .doOnNext(service -> logger.debug("Retrieved service: {}", service.getServiceName()))
                    .doOnComplete(() -> logger.info("Successfully retrieved all services from legacy configuration"))
                    .doOnError(error -> logger.error("Error retrieving services", error));
        }
    }

    @GetMapping("/services/{serviceName}/health")
    public Mono<ServiceInfo> checkServiceHealth(@PathVariable String serviceName) {
        logger.info("Checking health for service: {}", serviceName);

        ServiceConfig serviceConfig = getServiceConfig(serviceName);
        if (serviceConfig == null) {
            logger.warn("Service not found: {}", serviceName);
            return Mono.just(ServiceInfo.builder()
                    .serviceName(serviceName)
                    .status("NOT_FOUND")
                    .healthy(false)
                    .lastCheck(LocalDateTime.now())
                    .build());
        }

        String healthUrl = serviceConfig.getUri() + "/actuator/health";
        logger.debug("Checking health at URL: {}", healthUrl);

        return performRealHealthCheck(serviceName, healthUrl)
                .doOnSuccess(serviceInfo ->
                        logger.info("Health check completed for {}: status={}, healthy={}",
                                serviceName, serviceInfo.getStatus(), serviceInfo.isHealthy()))
                .doOnError(error ->
                        logger.error("Error checking health for service: {}", serviceName, error));
    }

    @GetMapping("/services/status")
    public Mono<Map<String, Object>> getAllServicesStatus() {
        logger.info("Fetching status for all services");

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("gateway", "UP");
            status.put("timestamp", LocalDateTime.now());

            if (servicesProperties != null && servicesProperties.getServices() != null) {
                // Verificar todos los servicios configurados
                List<Mono<ServiceInfo>> healthChecks = servicesProperties.getServices().entrySet().stream()
                        .map(entry -> {
                            ServiceConfig config = entry.getValue();
                            config.setName(entry.getKey());
                            return performRealHealthCheck(entry.getKey(), config.getUri() + "/actuator/health")
                                    .onErrorReturn(ServiceInfo.builder()
                                            .serviceName(entry.getKey())
                                            .status("DOWN")
                                            .healthy(false)
                                            .lastCheck(LocalDateTime.now())
                                            .build());
                        })
                        .collect(Collectors.toList());

                return Flux.merge(healthChecks)
                        .collectList()
                        .map(serviceInfos -> {
                            status.put("services", serviceInfos);
                            long healthyServices = serviceInfos.stream().mapToLong(s -> s.isHealthy() ? 1 : 0).sum();
                            status.put("healthy-services", healthyServices);
                            status.put("total-services", serviceInfos.size());
                            return status;
                        });
            } else {
                status.put("services", getLegacyConfiguredServices());
                return Mono.just(status);
            }

        } catch (Exception e) {
            logger.error("Error retrieving services status", e);
            throw e;
        }
    }

    @GetMapping("/services/{serviceName}/endpoints")
    public Mono<Map<String, Object>> getServiceEndpoints(@PathVariable String serviceName) {
        logger.info("Fetching endpoints for service: {}", serviceName);

        ServiceConfig serviceConfig = getServiceConfig(serviceName);
        if (serviceConfig == null) {
            return Mono.just(Map.of("error", "Service not found", "service", serviceName));
        }

        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("service", serviceName);
        endpoints.put("base-uri", serviceConfig.getUri());
        endpoints.put("routes", serviceConfig.getRoutes().stream()
                .collect(Collectors.toMap(
                        route -> route.getRouteId(),
                        route -> Map.of(
                                "path", route.getPath(),
                                "target", serviceConfig.getUri() + (route.getTargetPath() != null ? route.getTargetPath() : ""),
                                "module", route.getModule() != null ? route.getModule() : "unknown",
                                "requires-auth", route.isRequiresAuth()
                        )
                )));

        return Mono.just(endpoints);
    }

    // Método para verificación real de salud
    private Mono<ServiceInfo> performRealHealthCheck(String serviceName, String healthUrl) {
        long startTime = System.currentTimeMillis();

        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    String status = response.containsKey("status") ?
                            response.get("status").toString() : "UNKNOWN";
                    boolean isHealthy = "UP".equalsIgnoreCase(status);

                    return ServiceInfo.builder()
                            .serviceName(serviceName)
                            .serviceUrl(healthUrl.replace("/actuator/health", ""))
                            .status(status)
                            .healthy(isHealthy)
                            .responseTime(System.currentTimeMillis() - startTime)
                            .lastCheck(LocalDateTime.now())
                            .version(extractVersion(response))
                            .build();
                })
                .onErrorReturn(ServiceInfo.builder()
                        .serviceName(serviceName)
                        .serviceUrl(healthUrl.replace("/actuator/health", ""))
                        .status("DOWN")
                        .healthy(false)
                        .responseTime(System.currentTimeMillis() - startTime)
                        .lastCheck(LocalDateTime.now())
                        .build());
    }

    // Construir servicios desde configuración YAML
    private List<ServiceInfo> buildServicesFromConfig() {
        if (servicesProperties == null || servicesProperties.getServices() == null) {
            return new ArrayList<>();
        }

        return servicesProperties.getServices().entrySet().stream()
                .map(entry -> {
                    ServiceConfig config = entry.getValue();
                    config.setName(entry.getKey());

                    Map<String, String> endpoints = config.getRoutes().stream()
                            .collect(Collectors.toMap(
                                    route -> route.getRouteId(),
                                    route -> route.getPath()
                            ));

                    return ServiceInfo.builder()
                            .serviceName(entry.getKey())
                            .serviceUrl(config.getUri())
                            .port(config.getPort())
                            .status("UNKNOWN")
                            .endpoints(endpoints)
                            .lastCheck(LocalDateTime.now())
                            .requiresAuth(config.isRequiresAuth())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Helper methods
    private ServiceConfig getServiceConfig(String serviceName) {
        if (servicesProperties != null && servicesProperties.getServices() != null) {
            ServiceConfig config = servicesProperties.getServices().get(serviceName);
            if (config != null) {
                config.setName(serviceName);
            }
            return config;
        }
        return null;
    }

    private long countServicesWithAuth() {
        if (servicesProperties == null || servicesProperties.getServices() == null) {
            return 0;
        }
        return servicesProperties.getServices().values().stream()
                .mapToLong(config -> config.isRequiresAuth() ? 1 : 0)
                .sum();
    }

    private int countTotalEndpoints() {
        if (servicesProperties == null || servicesProperties.getServices() == null) {
            return 0;
        }
        return servicesProperties.getServices().values().stream()
                .mapToInt(config -> config.getRoutes().size())
                .sum();
    }

    private String extractVersion(Map<String, Object> response) {
        if (response.containsKey("version")) {
            return response.get("version").toString();
        }
        if (response.containsKey("build") && response.get("build") instanceof Map) {
            Map<String, Object> build = (Map<String, Object>) response.get("build");
            if (build.containsKey("version")) {
                return build.get("version").toString();
            }
        }
        return "unknown";
    }

    // Mantener métodos legacy para compatibilidad
    private List<ServiceInfo> getLegacyConfiguredServices() {
        // Tu implementación original aquí...
        return getConfiguredServices();
    }

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        logger.debug("Gateway health check requested");

        try {
            Map<String, String> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now().toString());

            logger.debug("Gateway health check: status=UP");
            return Mono.just(health);

        } catch (Exception e) {
            logger.error("Error during gateway health check", e);
            throw e;
        }
    }

    // Métodos de utilidad existentes (extractPath, extractFilters, etc.)
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

    private Map<String, String> extractFilters(org.springframework.cloud.gateway.route.Route route) {
        Map<String, String> filters = new HashMap<>();
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

    private Map<String, String> extractMetadata(org.springframework.cloud.gateway.route.Route route) {
        Map<String, String> metadata = new HashMap<>();
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

    // Mantener tu método original para compatibilidad hacia atrás
    private List<ServiceInfo> getConfiguredServices() {
        // Tu implementación original completa aquí...
        logger.debug("Building configured services list");
        List<ServiceInfo> services = new ArrayList<>();

        try {
            // Notification Service
            Map<String, String> notificationEndpoints = new HashMap<>();
            notificationEndpoints.put("send-email", "/api/notifications/email");
            notificationEndpoints.put("send-email-attachments", "/api/notifications/email/with-attachments");
            notificationEndpoints.put("send-push", "/api/notifications/push");
            notificationEndpoints.put("register-fcm", "/api/notifications/fcm/register");
            notificationEndpoints.put("unregister-fcm", "/api/notifications/fcm/unregister/{userId}");
            notificationEndpoints.put("history", "/api/notifications/history/{recipient}");
            notificationEndpoints.put("by-status", "/api/notifications/status/{status}");
            notificationEndpoints.put("by-id", "/api/notifications/{id}");
            notificationEndpoints.put("date-range", "/api/notifications/date-range");
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
                    .lastCheck(LocalDateTime.now())
                    .build());

            // Resto de tu implementación...

        } catch (Exception e) {
            logger.error("Error building configured services list", e);
            throw e;
        }

        return services;
    }
}