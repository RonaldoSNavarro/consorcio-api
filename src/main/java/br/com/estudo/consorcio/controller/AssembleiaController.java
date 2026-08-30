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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import br.com.estudo.consorcio.domain.model.StatusAssembleia;

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

    @Operation(summary = "Listar assembleias paginadas por status",
            description = "Consulta otimizada para a Central AGO. O status e obrigatorio para evitar carregar o historico integral.")
    @PreAuthorize("hasAuthority('VIEW_GRUPOS')")
    @GetMapping("/grupo/{grupoId}/status/{status}")
    public ResponseEntity<Page<AssembleiaResponseDTO>> listarPorGrupoEStatus(
            @PathVariable Long grupoId,
            @PathVariable StatusAssembleia status,
            @PageableDefault(size = 5, sort = "dataAssembleia") Pageable pageable) {
        return ResponseEntity.ok(service.listarPorGrupoEStatus(grupoId, status, pageable));
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
                    "(ATIVAS + CANCELADAS) usando exclusivamente a extração oficial da Loteria Federal " +
                    "elegível para a data da assembleia.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping("/{id}/apurar")
    public ResponseEntity<Map<String, Object>> apurar(
            @PathVariable Long id,
            @RequestBody(required = false) ApuracaoRequestDTO params) {
        boolean sorteioRealizado = params == null || !Boolean.FALSE.equals(params.realizarSorteio());
        service.apurarAssembleia(id, params);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Assembleia apurada e fechada com sucesso usando a extração oficial elegível da Loteria Federal.",
                "assembleiaId", id,
                "sorteioRealizado", sorteioRealizado
        ));
    }
}
