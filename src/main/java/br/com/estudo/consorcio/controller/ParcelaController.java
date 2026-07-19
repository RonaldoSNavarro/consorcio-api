package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.service.ParcelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/parcelas")
@Tag(name = "Parcelas", description = "Geração de boletos, registros de pagamento e motor de cálculo de inadimplência (juros pro-rata e multa).")
public class ParcelaController {

    private final ParcelaService service;

    public ParcelaController(ParcelaService service) {
        this.service = service;
    }

    @Operation(summary = "Gerar nova parcela", description = "Cria um novo registro de cobrança vinculando apenas ao ID da cota.")
    @PreAuthorize("hasAuthority('MANAGE_FINANCEIRO')")
    @PostMapping
    public ResponseEntity<ParcelaResponseDTO> cadastrar(@Valid @RequestBody ParcelaRequestDTO dto) {
        ParcelaResponseDTO parcelaSalva = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaSalva);
    }

    @Operation(summary = "Histórico de parcelas da cota", description = "Retorna a lista completa de parcelas de uma cota específica.")
    @PreAuthorize("hasAuthority('VIEW_FINANCEIRO')")
    @GetMapping("/cota/{cotaId}")
    public ResponseEntity<List<ParcelaResponseDTO>> listarPorCota(@PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }

    @Operation(summary = "Registrar pagamento da parcela", description = "Processa a baixa financeira, calculando automaticamente 2% de multa moratória e juros pro-rata die em caso de atraso.")
    @PreAuthorize("hasAuthority('MANAGE_FINANCEIRO')")
    @PutMapping("/{id}/pagar")
    public ResponseEntity<ParcelaResponseDTO> pagarParcela(@PathVariable Long id, @RequestParam LocalDate dataPagamento) {
        ParcelaResponseDTO parcelaPaga = service.pagar(id, dataPagamento);
        return ResponseEntity.ok(parcelaPaga);
    }

    @Operation(summary = "Amortizar lance (Redução de Prazo)", description = "Utiliza o lance pago para quitar as últimas parcelas do contrato (de trás para frente).")
    @PreAuthorize("hasAuthority('MANAGE_FINANCEIRO')")
    @PostMapping("/cota/{cotaId}/lance/reducao-prazo")
    public ResponseEntity<String> amortizarLanceReducaoPrazo(@PathVariable Long cotaId, @RequestParam BigDecimal valorLance) {
        service.amortizarPorReducaoDePrazo(cotaId, valorLance);
        return ResponseEntity.ok("Amortização por redução de prazo realizada com sucesso!");
    }

    @Operation(summary = "Amortizar lance (Diluição de Valor)", description = "Divide o lance pago igualmente entre todas as parcelas pendentes.")
    @PreAuthorize("hasAuthority('MANAGE_FINANCEIRO')")
    @PostMapping("/cota/{cotaId}/lance/diluicao")
    public ResponseEntity<String> amortizarLanceDiluicao(@PathVariable Long cotaId, @RequestParam BigDecimal valorLance) {
        service.amortizarPorDiluicao(cotaId, valorLance);
        return ResponseEntity.ok("Amortização por diluição do valor das parcelas realizada com sucesso!");
    }

    @Operation(summary = "Estornar pagamento da parcela", description = "Realiza o estorno contábil inverso (DÉBITO no fundo do grupo), zera os valores pagos e retorna a parcela para o status PENDENTE.")
    @PreAuthorize("hasAuthority('MANAGE_FINANCEIRO')")
    @PostMapping("/{id}/estornar")
    public ResponseEntity<ParcelaResponseDTO> estornarParcela(@PathVariable Long id) {
        ParcelaResponseDTO parcelaEstornada = service.estornar(id);
        return ResponseEntity.ok(parcelaEstornada);
    }
}