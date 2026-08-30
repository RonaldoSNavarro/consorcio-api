package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import br.com.estudo.consorcio.domain.model.TipoLance;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CredenciamentoLanceResponseDTO(
        Long id,
        Long cotaId,
        Integer codigoCota,
        Long assembleiaId,
        TipoLance tipoLance,
        ModalidadeLance modalidade,
        BigDecimal valorLance,
        BigDecimal valorFgts,
        BigDecimal valorEmbutido,
        BigDecimal valorProprio,
        StatusCredenciamento status,
        String hashAssinatura,
        LocalDateTime createdAt
) {}