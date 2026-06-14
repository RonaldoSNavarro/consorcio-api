package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceteResponseDTO(
        Long grupoId,
        String codigoGrupo,
        LocalDate dataReferencia,
        List<ContaSaldoDTO> contas
) {}
