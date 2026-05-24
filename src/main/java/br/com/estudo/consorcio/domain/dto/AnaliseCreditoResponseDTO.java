package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusAnalise;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AnaliseCreditoResponseDTO(
        Long id,
        Long cotaId,
        BigDecimal rendaComprovada,
        Boolean garantiaAprovada,
        StatusAnalise status,
        LocalDate dataAnalise,
        String observacao
) {
}
