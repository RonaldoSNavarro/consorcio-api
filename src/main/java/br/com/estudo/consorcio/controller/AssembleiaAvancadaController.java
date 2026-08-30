package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.SimulacaoApuracaoResponseDTO;
import br.com.estudo.consorcio.service.AssembleiaAvancadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assembleias-avancadas")
@Tag(name = "Motor Avançado de Assembleias", description = "Simulação dry-run de apuração, múltiplos sorteios e auditoria SHA-256")
public class AssembleiaAvancadaController {

    private final AssembleiaAvancadaService service;

    public AssembleiaAvancadaController(AssembleiaAvancadaService service) {
        this.service = service;
    }

    @Operation(summary = "Simular apuração em modo dry-run sem alterar o estado do banco")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/assembleia/{assembleiaId}/simular")
    public ResponseEntity<SimulacaoApuracaoResponseDTO> simular(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.simularApuracao(assembleiaId));
    }

    @Operation(summary = "Gerar e obter hash de auditoria SHA-256 da ata da assembleia")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/assembleia/{assembleiaId}/audit-hash")
    public ResponseEntity<String> obterHashAuditoria(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.gerarAuditTrailSha256(assembleiaId));
    }
}