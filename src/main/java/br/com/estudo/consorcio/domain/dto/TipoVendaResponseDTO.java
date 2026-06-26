package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.TipoVendaEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TipoVendaResponseDTO(
    Long id,
    String nome,
    String descricao,
    TipoVendaEnum canal,
    BigDecimal percentualComissao,
    Boolean exigeSeguro,
    Boolean permiteReajuste,
    Boolean ativo,
    LocalDateTime dataCriacao
) {}
