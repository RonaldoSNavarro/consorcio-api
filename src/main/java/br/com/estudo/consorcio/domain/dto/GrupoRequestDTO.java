package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados de entrada para criação de um novo grupo")
public record GrupoRequestDTO(
        @NotBlank(message = "Código é obrigatório")
        @Schema(example = "GRP-AUTO-002")
        String codigo,

        @NotNull(message = "Valor do crédito é obrigatório")
        @Positive(message = "Valor do crédito deve ser positivo")
        @Schema(example = "60000.00")
        BigDecimal valorCredito,

        @NotNull(message = "Prazo em meses é obrigatório")
        @Positive(message = "Prazo em meses deve ser positivo")
        @Schema(example = "60")
        Integer prazoMeses,

        @NotNull(message = "Taxa de administração é obrigatória")
        @Positive(message = "Taxa de administração deve ser positiva")
        @Schema(example = "15.00")
        BigDecimal taxaAdministracao
) {}