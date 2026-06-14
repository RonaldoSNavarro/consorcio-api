package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record ContaSaldoDTO(
        String codigoCosif,
        String nome,
        String natureza,
        BigDecimal saldo
) {}
