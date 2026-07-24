package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BemReferenciaRequestDTO(
    @NotNull(message = "Categoria do bem é obrigatória")
    Long categoriaBemId,

    @NotBlank(message = "Descrição do bem é obrigatória")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    String descricao,

    @NotNull(message = "Valor atual do bem é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal valorAtual,

    String codigoFipe,
    Boolean ativo
) {}
