package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record EvolucaoMensalDTO(
        String mes,
        Long vendas,
        Long contemplacoes,
        Long exclusoes,
        BigDecimal taxaInadimplencia
) {}