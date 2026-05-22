package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record CotaReembolsoResponseDTO(
        Long cotaId,
        Integer numeroCota,
        BigDecimal totalFundoComumPago,
        BigDecimal multaRescisoria,
        BigDecimal valorReembolsado,
        Boolean reembolsada
) {}
