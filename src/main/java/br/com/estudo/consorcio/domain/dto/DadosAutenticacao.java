package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DadosAutenticacao(
    @Schema(description = "Login do usuário", example = "admin")
    String login, 
    @Schema(description = "Senha do usuário", example = "admin")
    String senha
) {
}