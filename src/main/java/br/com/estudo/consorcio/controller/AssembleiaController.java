package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.service.AssembleiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assembleias")
public class AssembleiaController {

    private final AssembleiaService service;

    public AssembleiaController(AssembleiaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Assembleia> agendar(@RequestBody Assembleia assembleia) {
        Assembleia assembleiaSalva = service.salvar(assembleia);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembleiaSalva);
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<Assembleia>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }
}