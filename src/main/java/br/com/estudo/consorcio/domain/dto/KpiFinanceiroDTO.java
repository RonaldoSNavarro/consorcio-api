package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record KpiFinanceiroDTO(
        BigDecimal arrecadacaoTotal,
        BigDecimal metaArrecadacao,
        BigDecimal percentualAtingimento,
        BigDecimal taxaInadimplenciaMedia,
        BigDecimal ticketMedioCredito,
        Long totalContemplacoes,
        BigDecimal receitaTaxaAdminAcumulada,
        BigDecimal totalFundoComum,
        BigDecimal totalFundoReserva
) {}