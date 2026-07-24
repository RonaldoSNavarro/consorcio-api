package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record PropostaComplianceResponseDTO(
        Long id,
        String numeroProposta,
        BigDecimal valorCreditoSolicitado,
        ClienteResponseDTO cliente,
        ProdutoConsorcioResponseDTO produto,
        List<AlertaResumoDTO> alertas
) {
}
