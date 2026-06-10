package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusParcela;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaResponseDTO(
        Long id,
        Long cotaId,
        Integer numeroParcela,
        BigDecimal valorFundoComum,
        BigDecimal percentualFundoComum,
        BigDecimal valorTaxaAdministracao,
        BigDecimal valorFundoReserva,
        BigDecimal valorSeguro,
        BigDecimal valorParcela,
        BigDecimal valorMulta,
        BigDecimal valorJuros,
        BigDecimal valorPago,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        StatusParcela status
) {}