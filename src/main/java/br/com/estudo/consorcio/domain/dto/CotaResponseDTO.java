package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusCota;
import java.math.BigDecimal;
import br.com.estudo.consorcio.domain.enums.CategoriaBem;

public record CotaResponseDTO(
        Long id,
        Integer codigoCota,
        Long clienteId,
        Long grupoId,
        String codigoGrupo,
        String nomeConsorciado,
        BigDecimal percentualPago,
        BigDecimal percentualAPagar,
        Long bemReferenciaId,
        String nomeBemReferencia,
        BigDecimal valorBemReferencia,
        CategoriaBem categoriaBem,
        Integer prazoMeses,
        StatusCota status,
        Integer versaoHistorico
) {}