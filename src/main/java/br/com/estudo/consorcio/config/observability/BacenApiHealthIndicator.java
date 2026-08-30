package br.com.estudo.consorcio.config.observability;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class BacenApiHealthIndicator {

    public HealthCheckResult check() {
        long start = System.currentTimeMillis();
        // Verificação simulada de conectividade com gateway BACEN
        long latency = System.currentTimeMillis() - start;
        return new HealthCheckResult(
                "BACEN Regulatory Gateway",
                HealthStatus.UP,
                latency,
                Map.of("endpoint", "https://api.bcb.gov.br", "protocol", "HTTPS")
        );
    }
}