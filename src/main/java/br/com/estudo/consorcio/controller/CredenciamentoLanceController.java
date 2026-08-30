package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceResponseDTO;
import br.com.estudo.consorcio.service.CredenciamentoLanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credenciamentos")
@Tag(name = "Credenciamento de Lances", description = "Endpoints para registro prévio de lances, cancelamento e auditoria")
public class CredenciamentoLanceController {

    private final CredenciamentoLanceService service;

    public CredenciamentoLanceController(CredenciamentoLanceService service) {
        this.service = service;
    }

    @Operation(summary = "Credenciar intenção de lance para a próxima assembleia")
    @PreAuthorize("hasAnyRole('CONSORCIADO', 'ADMIN', 'CLIENTE') or hasAnyAuthority('VIEW_COTAS', 'ROLE_CONSORCIADO')")
    @PostMapping
    public ResponseEntity<CredenciamentoLanceResponseDTO> credenciar(
            @Valid @RequestBody CredenciamentoLanceRequestDTO dto,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.credenciarLance(dto, ip));
    }

    @Operation(summary = "Cancelar credenciamento de lance antes do fechamento da assembleia")
    @PreAuthorize("hasAnyRole('CONSORCIADO', 'ADMIN', 'CLIENTE') or hasAnyAuthority('VIEW_COTAS', 'ROLE_CONSORCIADO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelarCredenciamento(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar lances credenciados ativos de uma assembleia")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE', 'ANALISTA') or hasAuthority('ROLE_ADMIN')")
    @GetMapping("/assembleia/{assembleiaId}")
    public ResponseEntity<List<CredenciamentoLanceResponseDTO>> listarPorAssembleia(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.listarPorAssembleia(assembleiaId));
    }
}