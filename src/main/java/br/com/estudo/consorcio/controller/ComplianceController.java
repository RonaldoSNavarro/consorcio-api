package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AlertaComplianceResponseDTO;
import br.com.estudo.consorcio.domain.dto.DeliberarAlertaRequestDTO;
import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.service.ComplianceSincronizacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceSincronizacaoService sincronizacaoService;
    private final AlertaComplianceRepository alertaRepository;

    public ComplianceController(ComplianceSincronizacaoService sincronizacaoService,
                                AlertaComplianceRepository alertaRepository) {
        this.sincronizacaoService = sincronizacaoService;
        this.alertaRepository = alertaRepository;
    }

    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    public ResponseEntity<Map<String, Object>> sincronizarListasManualmente() {
        sincronizacaoService.sincronizarListas();

        return ResponseEntity.accepted().body(Map.of(
                "mensagem", "Sincronização de listas restritivas iniciada em background.",
                "dataHora", LocalDateTime.now()
        ));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    public ResponseEntity<List<AlertaComplianceResponseDTO>> listarAlertas(
            @RequestParam(required = false) StatusAlertaCompliance status) {

        List<AlertaCompliance> alertas;
        if (status != null) {
            alertas = alertaRepository.findByStatus(status);
        } else {
            alertas = alertaRepository.findAll();
        }

        List<AlertaComplianceResponseDTO> response = alertas.stream().map(a -> new AlertaComplianceResponseDTO(
                a.getId(),
                a.getCliente().getId(),
                a.getCliente().getNome(),
                a.getCliente().getCpfCnpj(),
                a.getListaRestritiva().getOrigem(),
                a.getListaRestritiva().getNome(),
                a.getScore(),
                a.getStatus(),
                a.getDataDeteccao(),
                a.getJustificativa()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/alertas/{alertaId}/deliberar")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    public ResponseEntity<Void> deliberarSobreAlerta(
            @PathVariable Long alertaId,
            @Valid @RequestBody DeliberarAlertaRequestDTO request) {

        AlertaCompliance alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));

        alerta.setStatus(request.novoStatus());
        alerta.setJustificativa(alerta.getJustificativa() + " | Deliberação: " + request.justificativa());
        alertaRepository.save(alerta);

        // RN-COMP-002: Bloqueio cautelar se CONFIRMADO.
        // Na prática injetariamos o ClienteService para fazer cotaService.bloquearCotas() etc.

        return ResponseEntity.ok().build();
    }
}
