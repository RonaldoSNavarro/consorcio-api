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
    @PreAuthorize("hasAuthority('VIEW_RELATORIOS')")
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
    @PreAuthorize("hasAuthority('VIEW_RELATORIOS')")
    public ResponseEntity<EstatisticasGrupoResponseDTO> gerarEstatisticas(
            @PathVariable Long grupoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        EstatisticasGrupoResponseDTO estatisticas = relatorioService.gerarEstatisticas(grupoId, dataInicio, dataFim);
        return ResponseEntity.ok(estatisticas);
    }

    @GetMapping("/pld-ft")
    @Operation(summary = "Alerta PLD/FT", description = "Lista todos os lances registrados acima de R$ 50.000,00 no período informado para monitoramento de lavagem de dinheiro.")
    @PreAuthorize("hasAuthority('VIEW_RELATORIOS')")
    public ResponseEntity<List<AlertaPldFtResponseDTO>> gerarAlertaPldFt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        
        List<AlertaPldFtResponseDTO> alertas = relatorioService.gerarAlertaPldFt(dataInicio, dataFim);
        return ResponseEntity.ok(alertas);
    }

    @GetMapping(value = "/balancete/{grupoId}/csv", produces = "text/csv")
    @Operation(summary = "Exportar Balancete Contábil (Doc 4110)", description = "Gera um arquivo CSV com o balancete.")
    @PreAuthorize("hasAuthority('VIEW_RELATORIOS')")
    public ResponseEntity<String> exportarBalanceteCsv(
            @PathVariable Long grupoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        
        if (dataReferencia == null) {
            dataReferencia = LocalDate.now();
        }
        
        BalanceteResponseDTO balancete = relatorioService.gerarBalancete(grupoId, dataReferencia);
        
        StringBuilder csv = new StringBuilder();
        csv.append("CodigoCOSIF;Conta;Natureza;Saldo\n");
        for (br.com.estudo.consorcio.domain.dto.ContaSaldoDTO c : balancete.contas()) {
            csv.append(c.codigoCosif()).append(";")
               .append(c.nome()).append(";")
               .append(c.natureza()).append(";")
               .append(c.saldo()).append("\n");
        }
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"balancete_" + grupoId + ".csv\"")
                .body(csv.toString());
    }

    @GetMapping(value = "/estatisticas/{grupoId}/csv", produces = "text/csv")
    @Operation(summary = "Exportar Estatísticas do Grupo (Doc 2080)", description = "Gera um arquivo CSV com as estatísticas.")
    @PreAuthorize("hasAuthority('VIEW_RELATORIOS')")
    public ResponseEntity<String> exportarEstatisticasCsv(
            @PathVariable Long grupoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        EstatisticasGrupoResponseDTO est = relatorioService.gerarEstatisticas(grupoId, dataInicio, dataFim);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Indicador;Valor\n");
        csv.append("Total Adesoes;").append(est.totalAdesoes()).append("\n");
        csv.append("Total Exclusoes;").append(est.totalExclusoes()).append("\n");
        csv.append("Lances Ofertados;").append(est.totalLancesOfertados()).append("\n");
        csv.append("Lances Vencedores;").append(est.totalLancesVencedores()).append("\n");
        csv.append("Contemplacoes Sorteio;").append(est.totalContemplacoesSorteio()).append("\n");
        csv.append("Contemplacoes Lance;").append(est.totalContemplacoesLance()).append("\n");
        csv.append("Cotas Inadimplentes;").append(est.totalCotasInadimplentes()).append("\n");
        csv.append("Valor Liberado;").append(est.valorTotalCreditosLiberados()).append("\n");
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"estatisticas_" + grupoId + ".csv\"")
                .body(csv.toString());
    }
}
