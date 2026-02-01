package com.api.gateway.modelConfig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GatewayMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> responseTimers = new ConcurrentHashMap<>();

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String serviceName, String endpoint) {
        String key = serviceName + ":" + endpoint;
        requestCounters.computeIfAbsent(key, k ->
                Counter.builder("gateway.requests.total")
                        .tag("service", serviceName)
                        .tag("endpoint", endpoint)
                        .register(meterRegistry)
        ).increment();
    }

    public Timer.Sample startTimer(String serviceName) {
        return Timer.start(meterRegistry);
    }

    public void recordResponseTime(Timer.Sample sample, String serviceName, int statusCode) {
        sample.stop(Timer.builder("gateway.response.time")
                .tag("service", serviceName)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry));
    }
}