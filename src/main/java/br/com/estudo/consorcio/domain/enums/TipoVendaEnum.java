package br.com.estudo.consorcio.domain.enums;

/**
 * Tipos de canal de venda de uma proposta de consórcio.
 * Cada tipo define comissão, fluxo de aprovação e documentação exigida.
 */
public enum TipoVendaEnum {
    /** Venda realizada diretamente por representante da administradora. */
    VENDA_DIRETA,
    /** Venda realizada através de correspondente bancário credenciado. */
    CORRESPONDENTE_BANCARIO,
    /** Venda online pelo portal do consorciado sem intermediário humano. */
    DIGITAL_SELF_SERVICE,
    /** Venda via parceiro comercial (lojas, concessionárias, imobiliárias). */
    PARCERIA_COMERCIAL
}
