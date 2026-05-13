package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de entrada para venda de uma nova cota")
public record CotaRequestDTO(
        @Schema(example = "15") Integer numeroCota,
        @Schema(example = "1") Long clienteId,
        @Schema(example = "2") Long grupoId
) {}