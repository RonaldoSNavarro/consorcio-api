package br.com.estudo.consorcio.domain.dto;

/**
 * Parâmetros externos para apuração de uma assembleia.
 * Permite indicar se deve realizar o sorteio nesta apuração.
 */
public record ApuracaoRequestDTO(
    /**
     * Campo legado, ignorado. O resultado é sempre obtido da extração oficial vinculada.
     */
    Integer dezenaSorteio,

    /**
     * Indica se o sorteio deve ser realizado nesta apuração,
     * além dos lances (livre e fixo).
     */
    Boolean realizarSorteio
) {}
