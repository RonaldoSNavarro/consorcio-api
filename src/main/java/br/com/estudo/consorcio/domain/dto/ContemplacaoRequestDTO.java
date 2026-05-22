package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoContemplacao;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Dados de entrada para registro de uma contemplação")
public record ContemplacaoRequestDTO(
        @NotNull(message = "ID da cota é obrigatório")
        @Positive(message = "ID da cota deve ser positivo")
        @Schema(example = "1")
        Long cotaId,

        @NotNull(message = "ID da assembleia é obrigatório")
        @Positive(message = "ID da assembleia deve ser positivo")
        @Schema(example = "1")
        Long assembleiaId,

        @NotNull(message = "Tipo de contemplação é obrigatório")
        @Schema(example = "LANCE_LIVRE")
        TipoContemplacao tipoContemplacao,

        @PositiveOrZero(message = "Valor do lance deve ser positivo ou zero")
        @Schema(example = "15000.00")
        BigDecimal valorLance,

        @NotNull(message = "Informar se é lance embutido é obrigatório")
        @Schema(example = "true")
        Boolean lanceEmbutido
) {}