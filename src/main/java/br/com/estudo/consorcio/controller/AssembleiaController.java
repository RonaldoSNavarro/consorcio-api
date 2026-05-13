package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.service.AssembleiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            description = "Registra um novo evento de assembleia (Ordinária ou Extraordinária) para um determinado grupo. Evento obrigatório para a realização de contemplações (sorteios ou lances).")
    @PostMapping
    public ResponseEntity<Assembleia> agendar(@RequestBody Assembleia assembleia) {
        Assembleia assembleiaSalva = service.salvar(assembleia);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembleiaSalva);
    }

    @Operation(summary = "Listar assembleias do grupo",
            description = "Retorna o histórico cronológico de todas as assembleias já realizadas ou agendadas para um grupo específico.")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<Assembleia>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }
}