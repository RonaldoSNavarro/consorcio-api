package br.com.estudo.consorcio.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContratoResponseDTO {
    private Long id;
    private String numeroContrato;
    private Long propostaId;
    private LocalDateTime dataAssinatura;
    private String status;
}
