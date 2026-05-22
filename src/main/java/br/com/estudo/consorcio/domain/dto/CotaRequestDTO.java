package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados de entrada para venda de uma nova cota")
public record CotaRequestDTO(
        @NotNull(message = "Número da cota é obrigatório")
        @Positive(message = "Número da cota deve ser positivo")
        @Schema(example = "15")
        Integer numeroCota,

        @NotNull(message = "ID do cliente é obrigatório")
        @Positive(message = "ID do cliente deve ser positivo")
        @Schema(example = "1")
        Long clienteId,

        @NotNull(message = "ID do grupo é obrigatório")
        @Positive(message = "ID do grupo deve ser positivo")
        @Schema(example = "2")
        Long grupoId
) {}