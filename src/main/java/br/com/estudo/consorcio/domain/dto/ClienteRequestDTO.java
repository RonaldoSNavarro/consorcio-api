package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados de entrada para cadastro de um novo cliente")
public record ClienteRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Schema(example = "João da Silva")
        String nome,

        @NotBlank(message = "CPF/CNPJ é obrigatório")
        @Pattern(regexp = "^\\d{11}$|^\\d{14}$", message = "CPF deve ter 11 dígitos ou CNPJ deve ter 14 dígitos")
        @Schema(example = "11122233344")
        String cpfCnpj,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Schema(example = "joao@email.com")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve ter 10 ou 11 dígitos")
        @Schema(example = "11999999999")
        String telefone
) {}