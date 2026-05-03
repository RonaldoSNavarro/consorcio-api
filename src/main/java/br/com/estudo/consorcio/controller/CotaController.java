package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.service.CotaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotas")
public class CotaController {

    private final CotaService service;

    public CotaController(CotaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cota> cadastrar(@RequestBody Cota cota) {
        Cota cotaSalva = service.salvar(cota);
        return ResponseEntity.status(HttpStatus.CREATED).body(cotaSalva);
    }

    @GetMapping
    public ResponseEntity<List<Cota>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Cota>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<Cota>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }
}