package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.MovimentoFinanceiroResponseDTO;
import br.com.estudo.consorcio.service.MovimentoFinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Movimentos Financeiros", description = "Módulo Financeiro - Consulta ao extrato de movimentações de grupos e cotas.")
public class MovimentoFinanceiroController {

    private final MovimentoFinanceiroService service;

    public MovimentoFinanceiroController(MovimentoFinanceiroService service) {
        this.service = service;
    }

    @Operation(summary = "Obter extrato financeiro do grupo",
            description = "Retorna todos os lançamentos de crédito e débito no fundo comum e de reserva do grupo.")
    @PreAuthorize("hasAuthority('VIEW_FINANCEIRO')")
    @GetMapping("/grupos/{grupoId}/movimentos")
    public ResponseEntity<List<MovimentoFinanceiroResponseDTO>> listarPorGrupo(
            @Parameter(description = "ID do grupo") @PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }

    @Operation(summary = "Obter extrato financeiro da cota",
            description = "Retorna todos os pagamentos e amortizações detalhados de uma cota específica.")
    @PreAuthorize("hasAuthority('VIEW_FINANCEIRO')")
    @GetMapping("/cotas/{cotaId}/movimentos")
    public ResponseEntity<List<MovimentoFinanceiroResponseDTO>> listarPorCota(
            @Parameter(description = "ID da cota") @PathVariable Long cotaId) {
        return ResponseEntity.ok(service.listarPorCota(cotaId));
    }

    @Operation(summary = "Obter saldo atual do grupo",
            description = "Calcula a soma das entradas (CRÉDITO) menos as saídas (DÉBITO) do grupo.")
    @PreAuthorize("hasAuthority('VIEW_FINANCEIRO')")
    @GetMapping("/grupos/{grupoId}/saldo")
    public ResponseEntity<BigDecimal> obterSaldoGrupo(
            @Parameter(description = "ID do grupo") @PathVariable Long grupoId) {
        return ResponseEntity.ok(service.obterSaldoGrupo(grupoId));
    }
}
