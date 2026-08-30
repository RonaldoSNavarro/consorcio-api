package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WebhookPixRequestDTO(
        @NotBlank(message = "txid é obrigatório")
        String txid,

        @NotNull(message = "valorPago é obrigatório")
        @Positive(message = "valorPago deve ser positivo")
        BigDecimal valorPago,

        BigDecimal taxaBancaria,
        String endToEndId
) {}