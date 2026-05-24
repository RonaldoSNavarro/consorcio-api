package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusApuracaoLance;
import br.com.estudo.consorcio.domain.model.TipoLance;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LanceResponseDTO(
    Long id,
    Long cotaId,
    Long assembleiaId,
    TipoLance tipo,
    BigDecimal valorOferta,
    LocalDateTime dataOferta,
    StatusApuracaoLance statusApuracao
) {}
