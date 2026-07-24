package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record GrupoFinanceiroDTO(
        Long grupoId,
        String codigoGrupo,
        BigDecimal totalFundoComum,
        BigDecimal totalTaxaAdm,
        BigDecimal totalFundoReserva
) {
}
