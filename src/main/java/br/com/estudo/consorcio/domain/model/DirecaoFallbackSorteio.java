package br.com.estudo.consorcio.domain.model;

/**
 * Direção de busca para fallback de contemplação quando a cota sorteada não está apta.
 */
public enum DirecaoFallbackSorteio {
    /**
     * Vai subindo; se chegar ao fim, volta do início descendo.
     */
    ACIMA_DEPOIS_ABAIXO,

    /**
     * Vai descendo; se chegar ao início, volta do fim subindo.
     */
    ABAIXO_DEPOIS_ACIMA,

    /**
     * Só sobe, sem circular.
     */
    SO_ACIMA,

    /**
     * Só desce, sem circular.
     */
    SO_ABAIXO
}
