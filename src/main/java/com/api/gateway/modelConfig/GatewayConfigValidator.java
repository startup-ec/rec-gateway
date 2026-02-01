package com.api.gateway.modelConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GatewayConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfigValidator.class);

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration(ApplicationReadyEvent event) {
        // Aquí puedes agregar validaciones de configuración
        logger.info("Validating gateway configuration...");
        // Validar puertos únicos, rutas no duplicadas, etc.
    }

    public boolean isValidServiceConfig(ServiceConfig config) {
        if (config.getHost() == null || config.getHost().isEmpty()) {
            logger.error("Service host cannot be empty");
            return false;
        }

        if (config.getPort() <= 0 || config.getPort() > 65535) {
            logger.error("Invalid port number: {}", config.getPort());
            return false;
        }

        return true;
    }
}