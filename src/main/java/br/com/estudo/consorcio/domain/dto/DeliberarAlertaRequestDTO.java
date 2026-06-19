package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliberarAlertaRequestDTO(
        @NotNull(message = "O novo status é obrigatório.")
        StatusAlertaCompliance novoStatus,

        @NotBlank(message = "A justificativa da deliberação é obrigatória.")
        String justificativa
) {}
