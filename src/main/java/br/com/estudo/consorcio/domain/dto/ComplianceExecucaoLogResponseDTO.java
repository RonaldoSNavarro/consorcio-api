package br.com.estudo.consorcio.domain.dto;

import java.time.LocalDateTime;

public record ComplianceExecucaoLogResponseDTO(
        Long id,
        LocalDateTime dataExecucao,
        String triggerExecucao,
        String ofacStatus,
        Integer pepRegistros,
        Integer onuRegistros,
        Integer ibgeRegistros,
        Integer ofacRegistros,
        Long duracaoMs,
        String erros
) {}
