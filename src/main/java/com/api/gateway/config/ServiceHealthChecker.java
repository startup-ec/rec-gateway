package com.api.gateway.config;

import com.api.gateway.modelConfig.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ServiceHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthChecker.class);
    private final WebClient webClient;

    public ServiceHealthChecker() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Scheduled(fixedRate = 30000) // Cada 30 segundos
    public void checkServicesHealth() {
        // Implementar verificación de salud de servicios
        logger.debug("Checking services health...");
    }

    public Mono<Boolean> isServiceHealthy(ServiceConfig serviceConfig) {
        String healthUrl = serviceConfig.getUri() + "/actuator/health";

        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("UP"))
                .onErrorReturn(false)
                .timeout(Duration.ofSeconds(5));
    }
}