package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ApuracaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.service.AssembleiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assembleias")
@Tag(name = "Assembleias", description = "Agendamento e histórico das reuniões de contemplação (Ordinárias e Extraordinárias).")
public class AssembleiaController {

    private final AssembleiaService service;

    public AssembleiaController(AssembleiaService service) {
        this.service = service;
    }

    @Operation(summary = "Agendar nova assembleia",
            description = "Registra um novo evento de assembleia vinculado a um grupo.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping
    public ResponseEntity<AssembleiaResponseDTO> agendar(@Valid @RequestBody AssembleiaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }

    @Operation(summary = "Listar assembleias do grupo")
    @PreAuthorize("hasAuthority('VIEW_GRUPOS')")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<AssembleiaResponseDTO>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }

    @Operation(summary = "Abrir captação de lances",
            description = "Transita o status da assembleia de AGENDADA para CAPTANDO.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping("/{id}/abrir-captacao")
    public ResponseEntity<Map<String, String>> abrirCaptacao(@PathVariable Long id) {
        service.abrirCaptacao(id);
        return ResponseEntity.ok(Map.of("mensagem", "Captação de lances aberta com sucesso."));
    }

    @Operation(summary = "Fechar captação de lances",
            description = "Transita o status da assembleia de CAPTANDO para REALIZADA.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping("/{id}/fechar-captacao")
    public ResponseEntity<Map<String, String>> fecharCaptacao(@PathVariable Long id) {
        service.fecharCaptacao(id);
        return ResponseEntity.ok(Map.of("mensagem", "Captação encerrada. Assembleia marcada como REALIZADA."));
    }

    @Operation(summary = "Apurar assembleia",
            description = "Executa o motor de apuração: processa lances livres, fixos e realiza sorteio " +
                    "(ATIVAS + CANCELADAS) se informado. A dezena da Loteria Federal ou Pedra Chave " +
                    "pode ser fornecida no body. Se omitida, usa valor aleatório.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping("/{id}/apurar")
    public ResponseEntity<Map<String, Object>> apurar(
            @PathVariable Long id,
            @RequestBody(required = false) ApuracaoRequestDTO params) {
        service.apurarAssembleia(id, params);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Assembleia apurada e fechada com sucesso.",
                "assembleiaId", id,
                "dezenaSorteio", params != null && params.dezenaSorteio() != null ? params.dezenaSorteio() : "Aleatória",
                "sorteioRealizado", params != null && Boolean.TRUE.equals(params.realizarSorteio())
        ));
    }
}