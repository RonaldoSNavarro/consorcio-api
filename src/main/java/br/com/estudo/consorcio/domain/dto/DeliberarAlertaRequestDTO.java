package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados de entrada para deliberação de um alerta de compliance")
public record DeliberarAlertaRequestDTO(
        @NotNull(message = "O novo status é obrigatório.")
        @Schema(example = "CONFIRMADO", description = "Novo status atribuído ao alerta (CONFIRMADO, FALSO_POSITIVO)")
        StatusAlertaCompliance novoStatus,

        @NotBlank(message = "A justificativa da deliberação é obrigatória.")
        @Schema(example = "Coincidência de dados na lista OFAC confirmada.", description = "Justificativa detalhada da deliberação")
        String justificativa
) {}
