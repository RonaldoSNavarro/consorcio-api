package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EstatisticasGrupoResponseDTO(
        Long grupoId,
        String codigoGrupo,
        LocalDate dataInicio,
        LocalDate dataFim,
        long totalAdesoes,
        long totalExclusoes,
        long totalLancesOfertados,
        long totalLancesVencedores,
        long totalContemplacoesSorteio,
        long totalContemplacoesLance,
        BigDecimal valorTotalCreditosLiberados
) {}
