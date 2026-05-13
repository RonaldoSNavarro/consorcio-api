package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de entrada para geração de uma nova cobrança")
public record ParcelaRequestDTO(
        @Schema(example = "1") Long cotaId,
        @Schema(example = "1") Integer numeroParcela,
        @Schema(example = "1000.00") BigDecimal valorFundoComum,
        @Schema(example = "150.00") BigDecimal valorTaxaAdministracao,
        @Schema(example = "50.00") BigDecimal valorFundoReserva,
        @Schema(example = "2026-06-10") LocalDate dataVencimento
) {}