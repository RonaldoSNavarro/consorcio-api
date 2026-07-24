package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.IndiceReajuste;

import java.math.BigDecimal;
import java.util.List;

public record SimulacaoReajusteResponseDTO(
        IndiceReajuste tipoIndice,
        BigDecimal percentualAcumulado12Meses,
        BigDecimal fatorReajuste,
        BigDecimal valorOriginal,
        BigDecimal novoValorCalculado,
        List<IndiceEconomicoDTO> historico12Meses
) {}
