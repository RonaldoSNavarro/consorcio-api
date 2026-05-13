package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoContemplacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados de entrada para registro de uma contemplação")
public record ContemplacaoRequestDTO(
        @Schema(example = "1") Long cotaId,
        @Schema(example = "1") Long assembleiaId,
        @Schema(example = "LANCE_LIVRE") TipoContemplacao tipoContemplacao,
        @Schema(example = "15000.00") BigDecimal valorLance,
        @Schema(example = "true") Boolean lanceEmbutido
) {}