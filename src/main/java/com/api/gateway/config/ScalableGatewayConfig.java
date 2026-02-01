package com.api.gateway.config;

import com.api.gateway.modelConfig.GatewayServicesProperties;
import com.api.gateway.modelConfig.ServiceConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ScalableGatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScalableGatewayConfig.class);

    @Autowired
    private GatewayServicesProperties servicesProperties;

    @Autowired
    private RouteBuilder routeBuilder;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        logger.info("Initializing scalable gateway routes configuration");

        RouteLocatorBuilder.Builder routesBuilder = builder.routes();

        // Construir rutas para todos los servicios configurados
        for (Map.Entry<String, ServiceConfig> entry : servicesProperties.getServices().entrySet()) {
            ServiceConfig serviceConfig = entry.getValue();
            serviceConfig.setName(entry.getKey()); // Asegurar que el nombre esté configurado

            routesBuilder = routeBuilder.buildServiceRoutes(routesBuilder, serviceConfig);
        }

        // Rutas internas del gateway
        routesBuilder = addGatewayInternalRoutes(routesBuilder);

        return routesBuilder.build();
    }

    private RouteLocatorBuilder.Builder addGatewayInternalRoutes(RouteLocatorBuilder.Builder builder) {
        logger.debug("Adding gateway internal routes");

        return builder
                // Salud del gateway
                .route("gateway-health", r -> r
                        .path("/gateway/health")
                        .uri("forward:/actuator/health"))

                // Gestión del gateway
                .route("gateway-management", r -> r
                        .path("/gateway/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Request", "internal")
                                .addResponseHeader("X-Gateway-Response", "internal"))
                        .uri("forward:/"));
    }

    @PostConstruct
    public void logConfigurationSummary() {
        logger.info("Scalable Gateway configuration initialized");
        logger.info("Configured services: {}", servicesProperties.getServices().keySet());

        servicesProperties.getServices().forEach((serviceName, config) -> {
            logger.info("  • Service: {} -> {}", serviceName, config.getUri());
            config.getRoutes().forEach(route ->
                    logger.info("    - {}: {} -> {}",
                            route.getRouteId(),
                            route.getPath(),
                            config.getUri() + (route.getTargetPath() != null ? route.getTargetPath() : "")
                    )
            );
        });

        logger.info("Gateway routes configuration completed successfully");
    }
}