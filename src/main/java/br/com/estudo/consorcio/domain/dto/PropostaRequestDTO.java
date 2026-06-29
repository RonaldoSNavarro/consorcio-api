package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropostaRequestDTO {

    @NotNull
    private Long clienteId;

    @NotNull
    private Long produtoId;

    @NotNull
    private Long tipoVendaId;

    @NotNull
    @Positive
    private BigDecimal valorCreditoSolicitado;
}
