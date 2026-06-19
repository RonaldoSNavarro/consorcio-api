package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import java.time.LocalDateTime;

public record AlertaComplianceResponseDTO(
        Long alertaId,
        Long clienteId,
        String nomeCliente,
        String cpfCnpj,
        OrigemListaRestritiva origemLista,
        String nomeEncontradoLista,
        java.math.BigDecimal scoreSimilaridade,
        StatusAlertaCompliance status,
        LocalDateTime dataDeteccao,
        String justificativa
) {}
