package com.api.gateway.modelConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteConfig {
    private String routeId;
    private String path;
    private String targetPath;
    private String module;
    private boolean requiresAuth;
    private Map<String, String> additionalHeaders;
}