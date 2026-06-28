package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.LanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.service.LanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lances")
@Tag(name = "Lances", description = "Endpoints para cadastro e gestão de ofertas de lances")
public class LanceController {

    private final LanceService service;

    public LanceController(LanceService service) {
        this.service = service;
    }

    @Operation(summary = "Registra uma nova oferta de lance",
            description = "Cadastra uma oferta de lance livre ou fixo para uma cota em uma assembleia aberta.")
    @PostMapping
    public ResponseEntity<LanceResponseDTO> registrar(@Valid @RequestBody LanceRequestDTO dto) {
        LanceResponseDTO salva = service.registrarLance(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @Operation(summary = "Registra sinistro de óbito e gera lance automático",
            description = "Resolução BCB 285: o seguro quita o saldo devedor e gera um lance na próxima AGO.")
    @PostMapping("/sinistro-obito/{cotaId}")
    public ResponseEntity<LanceResponseDTO> registrarSinistroObito(@PathVariable Long cotaId) {
        LanceResponseDTO salva = service.registrarSinistroObito(cotaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }
}
