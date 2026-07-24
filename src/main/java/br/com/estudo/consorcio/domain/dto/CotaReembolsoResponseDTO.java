package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record CotaReembolsoResponseDTO(
        Long cotaId,
        Integer codigoCota,
        BigDecimal totalFundoComumPago,
        BigDecimal multaRescisoria,
        BigDecimal valorReembolsado,
        Boolean reembolsada
) {}
