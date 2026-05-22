package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record CotaInadimplenciaResponseDTO(
        Long cotaId,
        Integer numeroCota,
        Boolean possuiInadimplencia,
        Integer quantidadeParcelasAtrasadas,
        BigDecimal valorOriginalAtrasado,
        BigDecimal multaAcumulada,
        BigDecimal jurosAcumulados,
        BigDecimal saldoDevedorTotal,
        List<ParcelaResponseDTO> parcelasAtrasadas
) {}
