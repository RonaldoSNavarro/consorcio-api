package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.service.CotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotas")
@Tag(name = "Cotas", description = "Gerenciamento das participações individuais, vinculação de clientes aos grupos e status da cota.")
public class CotaController {

    private final CotaService service;

    public CotaController(CotaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar cota", description = "Vincula um cliente a um grupo específico, gerando um número de cota único e definindo o status inicial como 'ATIVA'.")
    @PostMapping
    public ResponseEntity<CotaResponseDTO> cadastrar(@Valid @RequestBody CotaRequestDTO dto) {
        CotaResponseDTO cotaSalva = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cotaSalva);
    }

    @Operation(summary = "Listar cotas", description = "Exibe todas as cotas.")
    @GetMapping
    public ResponseEntity<List<CotaResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(summary = "Listar por cliente", description = "Lista cotas por cliente")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<CotaResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    @Operation(summary = "Listar por grupo", description = "Lista cotas por grupo.")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<CotaResponseDTO>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }
}