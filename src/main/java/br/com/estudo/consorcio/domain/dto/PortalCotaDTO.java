package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusCota;
import java.math.BigDecimal;

public record PortalCotaDTO(
        Long cotaId,
        String codigoGrupo,
        Integer codigoCota,
        StatusCota status,
        BigDecimal valorCredito,
        BigDecimal saldoDevedor,
        Integer parcelasPagas,
        Integer totalParcelas
) {}