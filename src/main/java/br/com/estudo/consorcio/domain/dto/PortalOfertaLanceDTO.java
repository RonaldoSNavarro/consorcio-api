package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PortalOfertaLanceDTO(
        @NotNull(message = "ID da cota é obrigatório")
        Long cotaId,

        @NotNull(message = "Valor do lance é obrigatório")
        @Positive(message = "Valor do lance deve ser positivo")
        BigDecimal valorLance,

        String tipoLance
) {}