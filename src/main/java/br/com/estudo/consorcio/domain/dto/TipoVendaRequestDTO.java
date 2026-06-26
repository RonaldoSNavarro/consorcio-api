package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.TipoVendaEnum;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TipoVendaRequestDTO(
    @NotBlank(message = "Nome do tipo de venda é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String nome,

    @Size(max = 500)
    String descricao,

    @NotNull(message = "Canal de venda é obrigatório")
    TipoVendaEnum canal,

    @NotNull(message = "Percentual de comissão é obrigatório")
    @DecimalMin(value = "0.0", message = "Comissão mínima é 0%")
    @DecimalMax(value = "0.20", message = "Comissão máxima é 20%")
    BigDecimal percentualComissao,

    Boolean exigeSeguro,
    Boolean permiteReajuste,
    Boolean ativo
) {}
