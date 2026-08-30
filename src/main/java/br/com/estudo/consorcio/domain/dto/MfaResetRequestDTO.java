package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaResetRequestDTO(
    @NotBlank(message = "A senha atual é obrigatória para confirmação de identidade.")
    String senhaAtual
) {}