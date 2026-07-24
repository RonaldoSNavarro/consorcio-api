package br.com.estudo.consorcio.domain.dto;

import java.time.LocalDateTime;

public record AlertaResumoDTO(
        String tipo,
        String descricao,
        LocalDateTime dataDeteccao
) {
}
