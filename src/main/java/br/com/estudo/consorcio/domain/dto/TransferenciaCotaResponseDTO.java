package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferenciaCotaResponseDTO(
        Long id,
        Long cotaId,
        String codigoGrupo,
        Integer codigoCota,
        Long cedenteId,
        String nomeCedente,
        String cpfCedente,
        Long cessionarioId,
        String nomeCessionario,
        String cpfCessionario,
        LocalDateTime dataSolicitacao,
        LocalDateTime dataEfetivacao,
        BigDecimal taxaTransferencia,
        StatusTransferencia status,
        String motivoRecusa,
        String observacao
) {}