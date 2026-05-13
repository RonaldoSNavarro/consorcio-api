package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Dados de entrada para agendamento de assembleia")
public record AssembleiaRequestDTO(
        @Schema(example = "2026-06-15") LocalDate dataAssembleia,
        @Schema(example = "ORDINARIA") TipoAssembleia tipo,
        @Schema(example = "1") Long grupoId
) {}