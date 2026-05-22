package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Schema(description = "Dados de entrada para agendamento de assembleia")
public record AssembleiaRequestDTO(
        @NotNull(message = "Data da assembleia é obrigatória")
        @FutureOrPresent(message = "Data da assembleia não pode ser no passado")
        @Schema(example = "2026-06-15")
        LocalDate dataAssembleia,

        @Schema(example = "ORDINARIA")
        TipoAssembleia tipo, // Pode ser nulo, pois o serviço define um padrão

        @NotNull(message = "ID do grupo é obrigatório")
        @Positive(message = "ID do grupo deve ser positivo")
        @Schema(example = "1")
        Long grupoId
) {}