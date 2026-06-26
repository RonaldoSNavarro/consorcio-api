package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.service.ContemplacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contemplacoes")
@Tag(name = "Contemplações", description = "Endpoints para registro de sorteios, lances e liberação de crédito")
public class ContemplacaoController {

    private final ContemplacaoService service;

    public ContemplacaoController(ContemplacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Registra uma nova contemplação",
            description = "Processa sorteios e lances. Valida regras do Banco Central, incluindo trava de saldo do Fundo Comum e limite máximo de 30% para lances embutidos.")
    @PostMapping
    public ResponseEntity<ContemplacaoResponseDTO> registrar(@Valid @RequestBody ContemplacaoRequestDTO dto) {
        ContemplacaoResponseDTO salva = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @Operation(summary = "Lista contemplados por Assembleia",
            description = "Retorna o histórico de todas as cotas contempladas em uma assembleia específica.")
    @GetMapping("/assembleia/{assembleiaId}")
    public ResponseEntity<List<ContemplacaoResponseDTO>> listarPorAssembleia(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.listarPorAssembleia(assembleiaId));
    }

    @Operation(summary = "Lista contemplacoes pendentes de integralizacao",
            description = "Retorna todas as cotas que estao aguardando o pagamento de lance livre ou fixo.")
    @GetMapping("/pendentes-integralizacao")
    public ResponseEntity<List<ContemplacaoResponseDTO>> listarPendentesIntegralizacao() {
        return ResponseEntity.ok(service.listarPendentesIntegralizacao());
    }

    @Operation(summary = "Pagar o bem da contemplação",
            description = "Realiza o pagamento/faturamento do bem (DÉBITO do valor do crédito liberado no fundo do grupo).")
    @PostMapping("/{id}/pagamento-bem")
    public ResponseEntity<ContemplacaoResponseDTO> pagarBem(@PathVariable Long id) {
        ContemplacaoResponseDTO response = service.pagarBem(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirmar integralização de lance livre",
            description = "Registra a compensação bancária do lance livre. Transita o status da cota para AGUARDANDO_ANALISE e gera os lançamentos contábeis no Ledger.")
    @PostMapping("/lances/{id}/integralizar")
    public ResponseEntity<CotaResponseDTO> confirmarIntegralizacao(@PathVariable Long id) {
        CotaResponseDTO response = service.confirmarPagamentoLance(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar contemplação por falta de pagamento",
            description = "Cancela a contemplação e o lance correspondente, voltando a cota para ATIVA.")
    @PostMapping("/lances/{id}/cancelar")
    public ResponseEntity<Void> cancelarContemplacao(@PathVariable Long id) {
        service.cancelarContemplacaoPorAtraso(id);
        return ResponseEntity.ok().build();
    }
}