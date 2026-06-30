package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record GrupoFinanceiroDTO(
        Long grupoId,
        String codigo,
        BigDecimal totalFundoComum,
        BigDecimal totalTaxaAdm,
        BigDecimal totalFundoReserva
) {
}
