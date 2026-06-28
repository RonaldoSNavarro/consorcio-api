package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.*;

/**
 * DTO para efetivação de uma venda de proposta de adesão.
 * Ao ser processado, cria uma Cota automaticamente no grupo informado
 * e vincula ao cliente.
 */
public record VendaPropostaRequestDTO(
    @NotNull(message = "Cliente é obrigatório")
    Long clienteId,

    @NotNull(message = "Valor do crédito desejado é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor do crédito deve ser maior que zero")
    java.math.BigDecimal valorCreditoDesejado,

    br.com.estudo.consorcio.domain.enums.CategoriaBem categoriaBem,
    
    Integer prazoMeses,

    @NotNull(message = "Tipo de venda é obrigatório")
    Long tipoVendaId,

    /** Se true, inclui seguro de vida na proposta. */
    Boolean contratarSeguro,

    /** Observações adicionais da proposta. */
    String observacoes
) {}
