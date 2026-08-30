package br.com.estudo.consorcio.config.observability;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HealthCheckService {

    private final BacenApiHealthIndicator bacenHealth;
    private final SmtpHealthIndicator smtpHealth;

    public HealthCheckService(BacenApiHealthIndicator bacenHealth, SmtpHealthIndicator smtpHealth) {
        this.bacenHealth = bacenHealth;
        this.smtpHealth = smtpHealth;
    }

    public List<HealthCheckResult> checkAll() {
        return List.of(bacenHealth.check(), smtpHealth.check());
    }
}