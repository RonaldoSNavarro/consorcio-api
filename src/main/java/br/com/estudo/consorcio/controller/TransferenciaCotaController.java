package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.TransferenciaCotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TransferenciaCotaResponseDTO;
import br.com.estudo.consorcio.service.TransferenciaCotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transferencias")
@Tag(name = "Transferência de Cotas", description = "Cessão de direitos, análise de crédito de cessionário e mudança de titularidade (Art. 13 Lei 11.795/2008)")
public class TransferenciaCotaController {

    private final TransferenciaCotaService service;

    public TransferenciaCotaController(TransferenciaCotaService service) {
        this.service = service;
    }

    @Operation(summary = "Solicitar transferência de cota")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping
    public ResponseEntity<TransferenciaCotaResponseDTO> solicitarTransferencia(@Valid @RequestBody TransferenciaCotaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarTransferencia(dto));
    }

    @Operation(summary = "Avaliar transferência (Aprovar ou Recusar análise de crédito)")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PutMapping("/{id}/avaliar")
    public ResponseEntity<TransferenciaCotaResponseDTO> avaliarTransferencia(
            @PathVariable Long id,
            @RequestParam boolean aprovado,
            @RequestParam(required = false) String motivoRecusa) {
        return ResponseEntity.ok(service.avaliarTransferencia(id, aprovado, motivoRecusa));
    }

    @Operation(summary = "Efetivar transferência de cota após pagamento da taxa")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PutMapping("/{id}/efetivar")
    public ResponseEntity<TransferenciaCotaResponseDTO> efetivarTransferencia(@PathVariable Long id) {
        return ResponseEntity.ok(service.efetivarTransferencia(id));
    }

    @Operation(summary = "Listar transferências por cota")
    @PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'MANAGE_COTAS')")
    @GetMapping("/cota/{cotaId}")
    public ResponseEntity<List<TransferenciaCotaResponseDTO>> listarPorCota(@PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }

    @Operation(summary = "Listar todas as transferências de cotas")
    @PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'MANAGE_COTAS')")
    @GetMapping
    public ResponseEntity<List<TransferenciaCotaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }
}