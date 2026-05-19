package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão para erros da API")
public record ExceptionDTO(
        @Schema(example = "Erro de validação")
        String mensagem,

        @Schema(example = "nome: Nome é obrigatório; email: Email inválido")
        String detalhes
) {}