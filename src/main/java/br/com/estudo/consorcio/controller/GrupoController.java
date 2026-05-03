package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.service.GrupoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Grupo> cadastrar(@RequestBody Grupo grupo) {
        Grupo grupoSalvo = service.salvar(grupo);
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoSalvo);
    }

    @GetMapping
    public ResponseEntity<List<Grupo>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}