package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record AnaliseCreditoRequestDTO(
        Long cotaId,
        BigDecimal rendaComprovada,
        Boolean garantiaAprovada,
        String observacao
) {
}
