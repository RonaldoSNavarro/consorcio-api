package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.validation.ValidCpfCnpj;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Dados de entrada para cadastro de um novo cliente")
public record ClienteRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Schema(example = "João da Silva")
        String nome,

        @NotBlank(message = "CPF/CNPJ é obrigatório")
        @ValidCpfCnpj(message = "CPF ou CNPJ inválido")
        @Schema(example = "11122233344")
        String cpfCnpj,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Schema(example = "joao@email.com")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve ter 10 ou 11 dígitos")
        @Schema(example = "11999999999")
        String telefone,

        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter exatamente 8 dígitos numéricos")
        @Schema(example = "01001000")
        String cep,

        @NotBlank(message = "Número é obrigatório")
        @Schema(example = "100")
        String numero,

        @Schema(example = "Apto 12")
        String complemento,

        @NotNull(message = "Patrimônio é obrigatório")
        @PositiveOrZero(message = "Patrimônio não pode ser negativo")
        @Schema(example = "150000.00")
        BigDecimal patrimonio,

        @NotNull(message = "Renda mensal é obrigatória")
        @PositiveOrZero(message = "Renda mensal não pode ser negativa")
        @Schema(example = "5500.00")
        BigDecimal rendaMensal,

        @NotNull(message = "Nível de risco é obrigatório")
        @Schema(example = "MEDIO")
        NivelRisco nivelRisco,

        @Schema(example = "false", description = "Indica se o cliente é Pessoa Politicamente Exposta (PEP)")
        Boolean pep
) {
    public ClienteRequestDTO(String nome, String cpfCnpj, String email, String telefone, String cep, String numero, String complemento, BigDecimal patrimonio, BigDecimal rendaMensal, NivelRisco nivelRisco) {
        this(nome, cpfCnpj, email, telefone, cep, numero, complemento, patrimonio, rendaMensal, nivelRisco, false);
    }
}