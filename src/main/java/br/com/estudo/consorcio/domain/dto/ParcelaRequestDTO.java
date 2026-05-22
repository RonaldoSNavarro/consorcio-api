package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de entrada para geração de uma nova cobrança")
public record ParcelaRequestDTO(
        @NotNull(message = "ID da cota é obrigatório")
        @Positive(message = "ID da cota deve ser positivo")
        @Schema(example = "1")
        Long cotaId,

        @NotNull(message = "Número da parcela é obrigatório")
        @Positive(message = "Número da parcela deve ser positivo")
        @Schema(example = "1")
        Integer numeroParcela,

        @NotNull(message = "Valor do fundo comum é obrigatório")
        @Positive(message = "Valor do fundo comum deve ser positivo")
        @Schema(example = "1000.00")
        BigDecimal valorFundoComum,

        @NotNull(message = "Valor da taxa de administração é obrigatório")
        @Positive(message = "Valor da taxa de administração deve ser positivo")
        @Schema(example = "150.00")
        BigDecimal valorTaxaAdministracao,

        @NotNull(message = "Valor do fundo de reserva é obrigatório")
        @Positive(message = "Valor do fundo de reserva deve ser positivo")
        @Schema(example = "50.00")
        BigDecimal valorFundoReserva,

        @NotNull(message = "Data de vencimento é obrigatória")
        @FutureOrPresent(message = "Data de vencimento não pode ser no passado")
        @Schema(example = "2026-06-10")
        LocalDate dataVencimento
) {}