package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Contemplacao;
import br.com.estudo.consorcio.service.ContemplacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contemplacoes")
public class ContemplacaoController {

    private final ContemplacaoService service;

    public ContemplacaoController(ContemplacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Contemplacao> registrar(@RequestBody Contemplacao contemplacao) {
        Contemplacao salva = service.registrar(contemplacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @GetMapping("/assembleia/{assembleiaId}")
    public ResponseEntity<List<Contemplacao>> listarPorAssembleia(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.listarPorAssembleia(assembleiaId));
    }
}