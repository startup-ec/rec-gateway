package com.api.gateway.controller;

import com.api.gateway.modelConfig.GatewayServicesProperties;
import com.api.gateway.modelConfig.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private GatewayServicesProperties servicesProperties;

    @Autowired
    private RouteLocator routeLocator;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();

        // Verificar que las properties se están cargando
        if (servicesProperties != null && servicesProperties.getServices() != null) {
            config.put("services", servicesProperties.getServices());
            config.put("totalServices", servicesProperties.getServices().size());

            // Verificar específicamente rec-cognito
            ServiceConfig cognitoConfig = servicesProperties.getServices().get("rec-cognito");
            if (cognitoConfig != null) {
                config.put("rec-cognito-config", cognitoConfig);
                config.put("rec-cognito-uri", cognitoConfig.getUri());
                config.put("rec-cognito-routes", cognitoConfig.getRoutes());
            } else {
                config.put("error", "rec-cognito service not found in configuration");
            }
        } else {
            config.put("error", "servicesProperties is null or empty");
        }

        return ResponseEntity.ok(config);
    }

    @GetMapping("/routes")
    public Flux<Map<String, Object>> getCurrentRoutes() {
        return routeLocator.getRoutes()
                .map(route -> {
                    Map<String, Object> routeInfo = new HashMap<>();
                    routeInfo.put("id", route.getId());
                    routeInfo.put("uri", route.getUri().toString());
                    routeInfo.put("predicate", route.getPredicate().toString());
                    routeInfo.put("filters", route.getFilters().size());
                    return routeInfo;
                });
    }
}

