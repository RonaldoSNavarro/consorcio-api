package br.com.estudo.consorcio.config.observability;

import java.util.Map;

public record HealthCheckResult(
        String service,
        HealthStatus status,
        long latencyMs,
        Map<String, Object> details
) {}