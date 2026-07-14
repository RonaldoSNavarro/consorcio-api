package br.com.estudo.consorcio.domain.dto;

public record MfaSetupResponse(
        String secret,
        String qrCodeImageUri
) {
}
