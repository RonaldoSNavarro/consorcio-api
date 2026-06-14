package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AlertaPldFtResponseDTO;
import br.com.estudo.consorcio.domain.dto.BalanceteResponseDTO;
import br.com.estudo.consorcio.domain.dto.EstatisticasGrupoResponseDTO;
import br.com.estudo.consorcio.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios BCB", description = "Endpoints para geração de relatórios exigidos pelo Banco Central")
@SecurityRequirement(name = "bearer-key")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/balancete/{grupoId}")
    @Operation(summary = "Gerar Balancete Contábil (Doc 4110)", description = "Retorna o saldo consolidado de todas as contas COSIF do grupo.")
    public ResponseEntity<BalanceteResponseDTO> gerarBalancete(
            @PathVariable Long grupoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        
        if (dataReferencia == null) {
            dataReferencia = LocalDate.now();
        }
        
        BalanceteResponseDTO balancete = relatorioService.gerarBalancete(grupoId, dataReferencia);
        return ResponseEntity.ok(balancete);
    }

    @GetMapping("/estatisticas/{grupoId}")
    @Operation(summary = "Estatísticas do Grupo (Doc 2080)", description = "Retorna o consolidado de adesões, exclusões, lances e contemplações no período.")
    public ResponseEntity<EstatisticasGrupoResponseDTO> gerarEstatisticas(
            @PathVariable Long grupoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        EstatisticasGrupoResponseDTO estatisticas = relatorioService.gerarEstatisticas(grupoId, dataInicio, dataFim);
        return ResponseEntity.ok(estatisticas);
    }

    @GetMapping("/pld-ft")
    @Operation(summary = "Alerta PLD/FT", description = "Lista todos os lances registrados acima de R$ 50.000,00 no período informado para monitoramento de lavagem de dinheiro.")
    public ResponseEntity<List<AlertaPldFtResponseDTO>> gerarAlertaPldFt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        
        List<AlertaPldFtResponseDTO> alertas = relatorioService.gerarAlertaPldFt(dataInicio, dataFim);
        return ResponseEntity.ok(alertas);
    }
}
