package com.api.gateway.config;

import com.api.gateway.filter.GatewayJwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
/*
@org.springframework.context.annotation.Configuration
public class GatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    @Autowired
    private GatewayJwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.context.annotation.Bean
    public org.springframework.cloud.gateway.route.RouteLocator customRouteLocator(
            org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder builder) {

        logger.info("Initializing custom gateway routes configuration");

        return builder.routes()
                // === NOTIFICATION SERVICE ROUTES ===

                // Ruta principal para API de notificaciones
                .route("notification-service-api", r -> {
                    logger.debug("Configuring route: notification-service-api for path /api/notifications/**");
                    return r.path("/api/notifications/**")
                            .filters(f -> {
                                logger.debug("Adding filters for notification-service-api route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "notification-service")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8081");
                })

                // Ruta para health/management de notificaciones
                .route("notification-service-health", r -> {
                    logger.debug("Configuring route: notification-service-health for path /notifications/**");
                    return r.path("/notifications/**")
                            .filters(f -> {
                                logger.debug("Adding health filters for notification-service-health route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "notification-service")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8081");
                })

                // Ping específico para notification service
                .route("notification-service-ping", r -> {
                    logger.debug("Configuring route: notification-service-ping for path /notifications/ping");
                    return r.path("/notifications/ping")
                            .filters(f -> {
                                logger.debug("Adding ping filters for notification-service-ping route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "notification-service")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8081/ping");
                })

                // === REC-COGNITO SERVICE ROUTES ===

                // Ruta para Auth endpoints
                .route("rec-cognito-auth", r -> {
                    logger.debug("Configuring route: rec-cognito-auth for path /api/v1/auth/**");
                    return r.path("/api/v1/auth/**")
                            .filters(f -> {
                                logger.debug("Adding authentication filters for rec-cognito-auth route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-cognito")
                                        .addRequestHeader("X-Module", "authentication")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8082");
                })

                // Ruta para Admin endpoints
                .route("rec-cognito-admin", r -> {
                    logger.debug("Configuring route: rec-cognito-admin for path /api/v1/admin/**");
                    return r.path("/api/v1/admin/**")
                            .filters(f -> {
                                logger.debug("Adding administration filters for rec-cognito-admin route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-cognito")
                                        .addRequestHeader("X-Module", "administration")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8082");
                })

                // Ruta para health y management de rec-cognito
                .route("rec-cognito-health", r -> {
                    logger.debug("Configuring route: rec-cognito-health for path /cognito/**");
                    return r.path("/cognito/**")
                            .filters(f -> {
                                logger.debug("Adding health filters for rec-cognito-health route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-cognito")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8082");
                })

                // Ping específico para rec-cognito
                .route("rec-cognito-ping", r -> {
                    logger.debug("Configuring route: rec-cognito-ping for path /cognito/ping");
                    return r.path("/cognito/ping")
                            .filters(f -> {
                                logger.debug("Adding ping filters for rec-cognito-ping route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-cognito")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8082/ping");
                })

                // === REC-LEAF-SCAN-CACAO SERVICE ROUTES ===

                // Ruta principal para API de cultivos
                .route("leaf-scan-cacao-cultivos", r -> {
                    logger.debug("Configuring protected route: leaf-scan-cacao-cultivos for path /api/v1/cultivos/**");
                    return r.path("/api/v1/cultivos/**")
                            .filters(f -> {
                                logger.debug("Adding JWT authentication and filters for leaf-scan-cacao-cultivos route");
                                return f
                                        .filter(jwtAuthenticationFilter) // ← JWT Authentication
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-leaf-scan-cacao")
                                        .addRequestHeader("X-Module", "cultivos")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8083");
                })

                // Ruta para health y management de leaf-scan-cacao
                .route("leaf-scan-cacao-health", r -> {
                    logger.debug("Configuring route: leaf-scan-cacao-health for path /leaf-scan/**");
                    return r.path("/leaf-scan/**")
                            .filters(f -> {
                                logger.debug("Adding health filters for leaf-scan-cacao-health route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-leaf-scan-cacao")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8083");
                })

                // Ping específico para leaf-scan-cacao
                .route("notification-service-ping", r -> {
                    logger.debug("Configuring route: notification-service-ping for path /notifications/ping");
                    return r.path("/notifications/ping")
                            .filters(f -> {
                                logger.debug("Adding ping filters for notification-service-ping route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "notification-service")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8081/ping");
                })

                .route("leaf-scan-cacao-ping", r -> {
                    logger.debug("Configuring route: leaf-scan-cacao-ping for path /leaf-scan/ping");
                    return r.path("/leaf-scan/ping")
                            .filters(f -> {
                                logger.debug("Adding ping filters for leaf-scan-cacao-ping route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "true")
                                        .addRequestHeader("X-Service-Name", "rec-leaf-scan-cacao")
                                        .addRequestHeader("X-Module", "health")
                                        .addResponseHeader("X-Gateway-Response", "processed");
                            })
                            .uri("http://localhost:8083/ping");
                })

                // === GATEWAY ROUTES ===

                // Ruta de salud del gateway
                .route("gateway-health", r -> {
                    logger.debug("Configuring route: gateway-health for path /gateway/health");
                    return r.path("/gateway/health")
                            .uri("forward:/actuator/health");
                })

                // Catch-all para rutas del gateway
                .route("gateway-management", r -> {
                    logger.debug("Configuring route: gateway-management for path /gateway/**");
                    return r.path("/gateway/**")
                            .filters(f -> {
                                logger.debug("Adding internal filters for gateway-management route");
                                return f
                                        .addRequestHeader("X-Gateway-Request", "internal")
                                        .addResponseHeader("X-Gateway-Response", "internal");
                            })
                            .uri("forward:/");
                })

                .build();
    }

    @PostConstruct
    public void logConfigurationSummary() {
        logger.info("Gateway configuration initialized with the following routes:");
        logger.info("  • Notification Service Routes:");
        logger.info("    - notification-service-api: /api/notifications/** -> http://localhost:8081");
        logger.info("    - notification-service-health: /notifications/** -> http://localhost:8081");
        logger.info("    - notification-service-ping: /notifications/ping -> http://localhost:8081/ping");
        logger.info("  • Rec-Cognito Service Routes:");
        logger.info("    - rec-cognito-auth: /api/v1/auth/** -> http://localhost:8082");
        logger.info("    - rec-cognito-admin: /api/v1/admin/** -> http://localhost:8082");
        logger.info("    - rec-cognito-health: /cognito/** -> http://localhost:8082");
        logger.info("    - rec-cognito-ping: /cognito/ping -> http://localhost:8082/ping");
        logger.info("  • Rec-Leaf-Scan-Cacao Service Routes:");
        logger.info("    - leaf-scan-cacao-cultivos: /api/v1/cultivos/** -> http://localhost:8083");
        logger.info("    - leaf-scan-cacao-health: /leaf-scan/** -> http://localhost:8083");
        logger.info("    - leaf-scan-cacao-ping: /leaf-scan/ping -> http://localhost:8083/ping");
        logger.info("  • Gateway Internal Routes:");
        logger.info("    - gateway-health: /gateway/health -> forward:/actuator/health");
        logger.info("    - gateway-management: /gateway/** -> forward:/");
        logger.info("Gateway routes configuration completed successfully");
    }
}*/