package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.service.ParcelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Gerar nova parcela", description = "Cria um novo registro de cobrança para a cota vinculada, calculando os valores iniciais de fundo comum, taxa de administração e fundo de reserva.")
    @PostMapping
    public ResponseEntity<Parcela> cadastrar(@RequestBody Parcela parcela) {
        Parcela parcelaSalva = service.salvar(parcela);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaSalva);
    }

    @Operation(summary = "Histórico de parcelas da cota", description = "Retorna a lista completa de parcelas (pagas e pendentes) de uma cota específica.")
    @GetMapping("/cota/{cotaId}")
    public ResponseEntity<List<Parcela>> listarPorCota(@PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }

    @Operation(summary = "Registrar pagamento da parcela", description = "Processa a baixa financeira. Caso o pagamento ocorra após o vencimento, o motor de inadimplência calcula automaticamente 2% de multa moratória e juros de mora de 1% ao mês pro-rata die (por dia de atraso).")
    @PutMapping("/{id}/pagar")
    public ResponseEntity<Parcela> pagarParcela(@PathVariable Long id, @RequestParam LocalDate dataPagamento) {

        Parcela parcelaPaga = service.pagar(id, dataPagamento);
        return ResponseEntity.ok(parcelaPaga);
    }

    @Operation(summary = "Amortizar lance (Redução de Prazo)", description = "Utiliza o valor do lance pago para quitar as últimas parcelas do contrato (de trás para frente). Mantém o valor da mensalidade intacto, mas reduz o tempo de permanência do cliente no grupo.")
    @PostMapping("/cota/{cotaId}/lance/reducao-prazo")
    public ResponseEntity<String> amortizarLanceReducaoPrazo(@PathVariable Long cotaId, @RequestParam BigDecimal valorLance) {

        service.amortizarPorReducaoDePrazo(cotaId, valorLance);

        return ResponseEntity.ok("Amortização por redução de prazo realizada com sucesso!");
    }

    @Operation(summary = "Amortizar lance (Diluição de Valor)", description = "Divide o valor do lance pago igualmente entre todas as parcelas pendentes. Reduz o valor mensal do boleto do cliente sem alterar o prazo final do grupo. Aplica tratamento matemático para dízimas e absorção de centavos residuais na última parcela.")
    @PostMapping("/cota/{cotaId}/lance/diluicao")
    public ResponseEntity<String> amortizarLanceDiluicao(@PathVariable Long cotaId, @RequestParam BigDecimal valorLance) {

        service.amortizarPorDiluicao(cotaId, valorLance);

        return ResponseEntity.ok("Amortização por diluição do valor das parcelas realizada com sucesso!");
    }
}