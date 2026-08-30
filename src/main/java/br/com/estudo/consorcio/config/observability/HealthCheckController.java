package br.com.estudo.consorcio.config.observability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Checks", description = "Monitoramento de integridade dos serviços e integrações externas")
public class HealthCheckController {

    private final HealthCheckService service;

    public HealthCheckController(HealthCheckService service) {
        this.service = service;
    }

    @Operation(summary = "Verificar integridade de todas as dependências e gateways")
    @GetMapping
    public ResponseEntity<List<HealthCheckResult>> checkHealth() {
        return ResponseEntity.ok(service.checkAll());
    }
}