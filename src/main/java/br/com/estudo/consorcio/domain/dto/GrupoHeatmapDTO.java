package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusGrupo;
import java.math.BigDecimal;

public record GrupoHeatmapDTO(
        Long grupoId,
        String codigoGrupo,
        StatusGrupo status,
        BigDecimal percentualInadimplencia,
        BigDecimal saldoFundoComum,
        String nivelCriticidade
) {}