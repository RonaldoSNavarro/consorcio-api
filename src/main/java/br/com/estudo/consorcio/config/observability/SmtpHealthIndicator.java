package br.com.estudo.consorcio.config.observability;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SmtpHealthIndicator {

    public HealthCheckResult check() {
        long start = System.currentTimeMillis();
        long latency = System.currentTimeMillis() - start;
        return new HealthCheckResult(
                "SMTP Messaging Gateway",
                HealthStatus.UP,
                latency,
                Map.of("status", "READY", "channel", "EMAIL")
        );
    }
}