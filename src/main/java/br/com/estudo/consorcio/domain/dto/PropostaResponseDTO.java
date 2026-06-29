package br.com.estudo.consorcio.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PropostaResponseDTO {
    private Long id;
    private String numeroProposta;
    private Long clienteId;
    private Long produtoId;
    private BigDecimal valorCreditoSolicitado;
    private String status;
    private LocalDateTime dataProposta;
}
