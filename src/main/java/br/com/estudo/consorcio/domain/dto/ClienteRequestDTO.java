package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de entrada para cadastro de um novo cliente")
public record ClienteRequestDTO(
        @Schema(example = "João da Silva") String nome,
        @Schema(example = "11122233344") String cpfCnpj,
        @Schema(example = "joao@email.com") String email,
        @Schema(example = "11999999999") String telefone
) {}