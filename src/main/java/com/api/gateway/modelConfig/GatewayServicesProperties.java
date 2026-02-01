package com.api.gateway.modelConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayServicesProperties {
    private Map<String, ServiceConfig> services = new HashMap<>();
}