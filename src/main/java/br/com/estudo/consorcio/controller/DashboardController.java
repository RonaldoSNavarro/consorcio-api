package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.EvolucaoMensalDTO;
import br.com.estudo.consorcio.domain.dto.GrupoHeatmapDTO;
import br.com.estudo.consorcio.domain.dto.KpiFinanceiroDTO;
import br.com.estudo.consorcio.domain.dto.ProjecaoReceitaDTO;
import br.com.estudo.consorcio.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard Analítico", description = "KPIs operacionais e financeiros, evolução mensal, projeções e mapa de calor dos grupos")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Obter KPIs consolidados do consórcio")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_FINANCEIRO')")
    @GetMapping("/kpis")
    public ResponseEntity<KpiFinanceiroDTO> obterKpis() {
        return ResponseEntity.ok(service.obterKpis());
    }

    @Operation(summary = "Obter evolução mensal de vendas, contemplações e inadimplência")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_FINANCEIRO')")
    @GetMapping("/tendencias")
    public ResponseEntity<List<EvolucaoMensalDTO>> obterTendencias() {
        return ResponseEntity.ok(service.obterTendencias());
    }

    @Operation(summary = "Obter heatmap de saúde dos grupos")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_GRUPOS')")
    @GetMapping("/grupos-status")
    public ResponseEntity<List<GrupoHeatmapDTO>> obterHeatmapGrupos() {
        return ResponseEntity.ok(service.obterHeatmapGrupos());
    }

    @Operation(summary = "Obter projeções de receita de taxa de administração")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_FINANCEIRO')")
    @GetMapping("/projecoes")
    public ResponseEntity<List<ProjecaoReceitaDTO>> obterProjecoes() {
        return ResponseEntity.ok(service.obterProjecoes());
    }
}