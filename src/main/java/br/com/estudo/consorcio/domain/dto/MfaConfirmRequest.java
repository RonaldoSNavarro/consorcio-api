package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MfaConfirmRequest(
        @NotBlank(message = "O código MFA é obrigatório")
        @Size(min = 6, max = 6, message = "O código deve ter 6 dígitos")
        String code
) {
}
