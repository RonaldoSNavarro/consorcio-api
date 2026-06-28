package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Dados para transferência de titularidade da cota")
public record TransferirCotaRequestDTO(
        @NotNull(message = "ID do novo cliente é obrigatório")
        @Schema(example = "2")
        Long novoClienteId,

        @NotBlank(message = "Motivo da transferência é obrigatório")
        @Schema(example = "Venda da cota para terceiro")
        String motivo,
        
        @Schema(description = "Taxa de transferência cobrada (opcional)")
        BigDecimal taxaTransferencia
) {}
