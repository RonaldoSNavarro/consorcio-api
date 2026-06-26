package br.com.estudo.consorcio.domain.dto;

/**
 * Parâmetros externos para apuração de uma assembleia.
 * Permite informar a dezena da Loteria Federal ou Pedra Chave
 * e indicar se deve realizar o sorteio nesta apuração.
 */
public record ApuracaoRequestDTO(
    /**
     * Dezena do prêmio principal da Loteria Federal ou número da Pedra Chave.
     * Se nulo, utiliza o valor já salvo na assembleia ou um número aleatório.
     */
    Integer dezenaSorteio,

    /**
     * Indica se o sorteio deve ser realizado nesta apuração,
     * além dos lances (livre e fixo).
     */
    Boolean realizarSorteio
) {}
