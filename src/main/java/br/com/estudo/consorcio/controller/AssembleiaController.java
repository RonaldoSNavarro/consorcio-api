package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.service.AssembleiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assembleias")
@Tag(name = "Assembleias", description = "Agendamento e histórico das reuniões de contemplação (Ordinárias e Extraordinárias).")
public class AssembleiaController {

    private final AssembleiaService service;

    public AssembleiaController(AssembleiaService service) {
        this.service = service;
    }

    @Operation(summary = "Agendar nova assembleia",
            description = "Registra um novo evento de assembleia vinculado a um grupo. O status inicial é 'ORDINARIA' por padrão caso não informado.")
    @PostMapping
    public ResponseEntity<AssembleiaResponseDTO> agendar(@Valid @RequestBody AssembleiaRequestDTO dto) {
        AssembleiaResponseDTO response = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar assembleias do grupo",
            description = "Retorna o histórico cronológico de todas as assembleias de um grupo específico através do seu ID.")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<AssembleiaResponseDTO>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }
}