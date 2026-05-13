package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Dados de entrada para criação de um novo grupo")
public record GrupoRequestDTO(
        @Schema(example = "GRP-AUTO-002") String codigo,
        @Schema(example = "60000.00") BigDecimal valorCredito,
        @Schema(example = "60") Integer prazoMeses,
        @Schema(example = "15.00") BigDecimal taxaAdministracao
) {}