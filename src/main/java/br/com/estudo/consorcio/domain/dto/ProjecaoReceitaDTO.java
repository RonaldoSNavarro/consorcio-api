package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record ProjecaoReceitaDTO(
        String mes,
        BigDecimal receitaProjetadaTA,
        Long cotasAtivasPrevistas,
        BigDecimal descontoInadimplenciaEstimado
) {}