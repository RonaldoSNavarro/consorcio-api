package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoValorResponseDTO(
    Long id,
    Long bemReferenciaId,
    String descricaoBem,
    BigDecimal valorAnterior,
    BigDecimal valorNovo,
    String origemReajuste,
    String codigoFipe,
    LocalDateTime dataAtualizacao
) {}
