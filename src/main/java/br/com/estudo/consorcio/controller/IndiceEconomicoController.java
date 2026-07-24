package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.dto.IndiceEconomicoDTO;
import br.com.estudo.consorcio.domain.dto.SimulacaoReajusteResponseDTO;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.service.BcbSgsService;
import br.com.estudo.consorcio.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/indices-economicos")
@Tag(name = "Índices Econômicos BACEN", description = "Consulta de séries temporais do BACEN SGS (INCC-M, IPCA, IGP-M), simulação e reajuste por índice.")
public class IndiceEconomicoController {

    private final BcbSgsService bcbService;
    private final GrupoService grupoService;

    public IndiceEconomicoController(BcbSgsService bcbService, GrupoService grupoService) {
        this.bcbService = bcbService;
        this.grupoService = grupoService;
    }

    @Operation(summary = "Obter variação dos últimos 12 meses do índice BACEN")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/{tipoIndice}/ultimos-12-meses")
    public ResponseEntity<List<IndiceEconomicoDTO>> obterUltimos12Meses(@PathVariable IndiceReajuste tipoIndice) {
        return ResponseEntity.ok(bcbService.buscarAtualizarUltimos12Meses(tipoIndice));
    }

    @Operation(summary = "Simular reajuste com base no acumulado de 12 meses do índice BACEN")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/simular")
    public ResponseEntity<SimulacaoReajusteResponseDTO> simularReajuste(
            @RequestParam IndiceReajuste tipoIndice,
            @RequestParam BigDecimal valorAtual) {
        return ResponseEntity.ok(bcbService.simularReajuste(tipoIndice, valorAtual));
    }

    @Operation(summary = "Reajustar grupo pelo acumulado do índice BACEN")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping("/grupos/{grupoId}/reajustar")
    public ResponseEntity<GrupoResponseDTO> reajustarGrupo(
            @PathVariable Long grupoId,
            @RequestParam(required = false) IndiceReajuste tipoIndice) {
        return ResponseEntity.ok(grupoService.reajustarGrupoPorIndice(grupoId, tipoIndice));
    }
}
