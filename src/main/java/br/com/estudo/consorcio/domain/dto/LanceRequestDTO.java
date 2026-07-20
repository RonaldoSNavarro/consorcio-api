package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoLance;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

public record LanceRequestDTO(
    @Schema(description = "ID da Cota", example = "1")
    @NotNull Long cotaId,
    @Schema(description = "ID da Assembleia", example = "1")
    @NotNull Long assembleiaId,
    @Schema(description = "Tipo de lance", example = "LIVRE")
    @NotNull TipoLance tipo,
    @Schema(description = "Valor da oferta (apenas para lance livre)", example = "15000.00")
    BigDecimal valorOferta,
    @Schema(description = "Modalidade de pagamento do lance", example = "ESPECIE")
    ModalidadeLance modalidade
) {}
