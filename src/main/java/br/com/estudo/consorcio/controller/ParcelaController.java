package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.service.ParcelaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcelas")
public class ParcelaController {

    private final ParcelaService service;

    public ParcelaController(ParcelaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Parcela> cadastrar(@RequestBody Parcela parcela) {
        Parcela parcelaSalva = service.salvar(parcela);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaSalva);
    }

    // Endpoint específico para a ação de pagar. Ex: PUT /api/parcelas/1/pagar
    @PutMapping("/{id}/pagar")
    public ResponseEntity<Parcela> pagarParcela(@PathVariable Long id) {
        Parcela parcelaPaga = service.pagar(id);
        return ResponseEntity.ok(parcelaPaga);
    }

    @GetMapping("/cota/{cotaId}")
    public ResponseEntity<List<Parcela>> listarPorCota(@PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }
}