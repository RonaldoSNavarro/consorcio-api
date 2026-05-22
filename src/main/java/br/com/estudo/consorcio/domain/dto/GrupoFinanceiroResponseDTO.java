package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record GrupoFinanceiroResponseDTO(
        Long grupoId,
        String codigoGrupo,
        BigDecimal totalFundoComumArrecadado,
        BigDecimal totalTaxaAdministracaoArrecadada,
        BigDecimal totalFundoReservaArrecadado,
        BigDecimal totalCreditosLiberados,
        BigDecimal saldoDisponivelFundoComum,
        BigDecimal saldoDisponivelFundoReserva
) {}
