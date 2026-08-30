package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimulacaoApuracaoResponseDTO(
        Long assembleiaId,
        Long grupoId,
        BigDecimal saldoFundoComum,
        Integer totalSorteadosPrevistos,
        List<Integer> cotasSorteioSimuladas,
        Integer totalLancesPrevistos,
        List<Integer> cotasLancesSimuladas,
        BigDecimal saldoRestanteProjetado,
        String hashPreviaSha256
) {}