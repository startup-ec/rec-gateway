package com.api.gateway.config;

import com.api.gateway.filter.GatewayJwtAuthenticationFilter;
import com.api.gateway.modelConfig.RouteConfig;
import com.api.gateway.modelConfig.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RouteBuilder.class);

    @Autowired
    private GatewayJwtAuthenticationFilter jwtAuthenticationFilter;

    public RouteLocatorBuilder.Builder buildServiceRoutes(
            RouteLocatorBuilder.Builder builder,
            ServiceConfig serviceConfig) {

        logger.info("Building routes for service: {}", serviceConfig.getName());

        for (RouteConfig route : serviceConfig.getRoutes()) {
            builder = addRoute(builder, serviceConfig, route);
        }

        return builder;
    }

    private RouteLocatorBuilder.Builder addRoute(
            RouteLocatorBuilder.Builder builder,
            ServiceConfig serviceConfig,
            RouteConfig routeConfig) {

        String routeId = String.format("%s-%s", serviceConfig.getName(), routeConfig.getRouteId());

        return builder.route(routeId, r -> {
            logger.debug("Configuring route: {} for path {}", routeId, routeConfig.getPath());

            return r.path(routeConfig.getPath())
                    .filters(f -> buildFilters(f, serviceConfig, routeConfig))
                    .uri(getTargetUri(serviceConfig, routeConfig));
        });
    }

    private GatewayFilterSpec buildFilters(
            GatewayFilterSpec filterSpec,
            ServiceConfig serviceConfig,
            RouteConfig routeConfig) {

        logger.debug("Adding filters for route: {}-{}", serviceConfig.getName(), routeConfig.getRouteId());

        // Autenticación JWT si es requerida
        if (routeConfig.isRequiresAuth() || serviceConfig.isRequiresAuth()) {
            filterSpec = filterSpec.filter(jwtAuthenticationFilter);
        }

        // Headers básicos del gateway
        filterSpec = filterSpec
                .addRequestHeader("X-Gateway-Request", "true")
                .addRequestHeader("X-Service-Name", serviceConfig.getName())
                .addResponseHeader("X-Gateway-Response", "processed");

        // Headers del módulo
        if (routeConfig.getModule() != null) {
            filterSpec = filterSpec.addRequestHeader("X-Module", routeConfig.getModule());
        }

        // Headers personalizados del servicio
        if (serviceConfig.getCustomHeaders() != null) {
            for (Map.Entry<String, String> header : serviceConfig.getCustomHeaders().entrySet()) {
                filterSpec = filterSpec.addRequestHeader(header.getKey(), header.getValue());
            }
        }

        // Headers adicionales de la ruta
        if (routeConfig.getAdditionalHeaders() != null) {
            for (Map.Entry<String, String> header : routeConfig.getAdditionalHeaders().entrySet()) {
                filterSpec = filterSpec.addRequestHeader(header.getKey(), header.getValue());
            }
        }

        return filterSpec;
    }

    private String getTargetUri(ServiceConfig serviceConfig, RouteConfig routeConfig) {
        String baseUri = serviceConfig.getUri();
        if (routeConfig.getTargetPath() != null && !routeConfig.getTargetPath().isEmpty()) {
            return baseUri + routeConfig.getTargetPath();
        }
        return baseUri;
    }
}