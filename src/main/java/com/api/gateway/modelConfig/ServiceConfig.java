package com.api.gateway.modelConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceConfig {
    private String name;
    private String host;
    private int port;
    private String basePath;
    private boolean requiresAuth;
    private String healthEndpoint;
    private Map<String, String> customHeaders;
    private List<RouteConfig> routes;

    public String getUri() {
        if (host != null && (host.startsWith("http://") || host.startsWith("https://"))) {
            return host + (port != 80 && port != 443 ? ":" + port : "");
        }
        return "http://" + (host != null ? host : "localhost") + ":" + port;
    }
}