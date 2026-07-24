package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BemReferenciaResponseDTO(
    Long id,
    Long categoriaBemId,
    String nomeCategoria,
    String tipoBacen,
    String indiceReajustePadrao,
    String descricao,
    BigDecimal valorAtual,
    LocalDate dataUltimaAtualizacao,
    String codigoFipe,
    Boolean ativo
) {}
