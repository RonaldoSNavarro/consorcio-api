package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrupoEncerrarResponseDTO(
        Long grupoId,
        String codigo,
        long totalParcelasBaixadas,
        BigDecimal valorTotalPDD,
        BigDecimal valorTransferidoRNP,
        LocalDate dataEncerramento
) {}
