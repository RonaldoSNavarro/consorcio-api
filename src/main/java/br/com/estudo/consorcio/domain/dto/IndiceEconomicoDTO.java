package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.IndiceReajuste;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndiceEconomicoDTO(
        IndiceReajuste tipoIndice,
        LocalDate dataReferencia,
        BigDecimal valorPercentual
) {}
