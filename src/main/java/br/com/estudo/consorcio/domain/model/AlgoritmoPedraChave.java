package br.com.estudo.consorcio.domain.model;

/**
 * Algoritmos para cálculo da pedra-chave a partir da Loteria Federal.
 */
public enum AlgoritmoPedraChave {
    /**
     * Os últimos 2, 3 ou 4 dígitos do 1º prêmio, dependendo do tamanho do grupo.
     */
    CENTENA,

    /**
     * Fração decimal da divisão do 1º prêmio pelo total de cotas ativas multiplicada pelo total de cotas.
     */
    DIVISAO_TOTAL,

    /**
     * Dígito da centena do 1º prêmio dividido por 1000, multiplicado pela maior cota ativa.
     */
    DIVISAO_1000
}
