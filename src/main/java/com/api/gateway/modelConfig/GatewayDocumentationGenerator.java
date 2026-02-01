package com.api.gateway.modelConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GatewayDocumentationGenerator {

    @Autowired
    private GatewayServicesProperties servicesProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void generateDocumentation() {
        StringBuilder doc = new StringBuilder();
        doc.append("# Gateway Routes Documentation\n\n");

        servicesProperties.getServices().forEach((serviceName, config) -> {
            doc.append("## ").append(serviceName.toUpperCase()).append("\n");
            doc.append("- **Host**: ").append(config.getHost()).append(":").append(config.getPort()).append("\n");
            doc.append("- **Authentication Required**: ").append(config.isRequiresAuth() ? "Yes" : "No").append("\n\n");

            doc.append("### Routes:\n");
            config.getRoutes().forEach(route -> {
                doc.append("- **").append(route.getPath()).append("** -> ")
                        .append(config.getUri())
                        .append(route.getTargetPath() != null ? route.getTargetPath() : "")
                        .append(" (Module: ").append(route.getModule()).append(")\n");
            });
            doc.append("\n");
        });

        // Aquí podrías escribir la documentación a un archivo
        System.out.println(doc.toString());
    }
}