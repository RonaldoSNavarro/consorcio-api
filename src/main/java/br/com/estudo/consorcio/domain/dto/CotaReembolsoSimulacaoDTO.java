package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotaReembolsoSimulacaoDTO(
        Long id,
        Integer codigoCota,
        Long clienteId,
        String clienteNome,
        String cpfCnpj,
        String numeroAssembleiaAGO,
        LocalDate dataContemplacaoAGO,
        BigDecimal valorBemReferenciaAGO,
        BigDecimal percentualFundoComumPago,
        BigDecimal valorHistoricoPago,
        BigDecimal valorBrutoRestituicao,
        BigDecimal valorMultaRestituicao,
        BigDecimal valorLiquidoRestituicao
) {}
