package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferenciaCotaRequestDTO(
        @NotNull(message = "ID da cota é obrigatório")
        @Positive(message = "ID da cota deve ser positivo")
        Long cotaId,

        @NotNull(message = "ID do cessionário é obrigatório")
        @Positive(message = "ID do cessionário deve ser positivo")
        Long cessionarioId,

        BigDecimal taxaTransferencia,
        String observacao
) {}