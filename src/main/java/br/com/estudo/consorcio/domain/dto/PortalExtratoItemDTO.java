package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusParcela;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PortalExtratoItemDTO(
        Long parcelaId,
        Integer numeroParcela,
        LocalDate dataVencimento,
        BigDecimal valorParcela,
        BigDecimal valorPago,
        StatusParcela status,
        LocalDate dataPagamento
) {}