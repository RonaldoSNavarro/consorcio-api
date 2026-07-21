package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;

public record ProdutoConsorcioResponseDTO(
        Long id,
        String nome,
        Integer prazoMeses,
        BigDecimal taxaAdministracaoPerc
) {
}
