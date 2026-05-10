package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.service.ParcelaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @GetMapping("/cota/{cotaId}")
    public ResponseEntity<List<Parcela>> listarPorCota(@PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<Parcela> pagarParcela(
            @PathVariable Long id,
            @RequestParam LocalDate dataPagamento) {

        Parcela parcelaPaga = service.pagar(id, dataPagamento);
        return ResponseEntity.ok(parcelaPaga);
    }

    @PostMapping("/cota/{cotaId}/lance/reducao-prazo")
    public ResponseEntity<String> amortizarLanceReducaoPrazo(
            @PathVariable Long cotaId,
            @RequestParam BigDecimal valorLance) {

        service.amortizarPorReducaoDePrazo(cotaId, valorLance);

        return ResponseEntity.ok("Amortização por redução de prazo realizada com sucesso!");
    }

    @PostMapping("/cota/{cotaId}/lance/diluicao")
    public ResponseEntity<String> amortizarLanceDiluicao(
            @PathVariable Long cotaId,
            @RequestParam BigDecimal valorLance) {

        service.amortizarPorDiluicao(cotaId, valorLance);

        return ResponseEntity.ok("Amortização por diluição do valor das parcelas realizada com sucesso!");
    }
}