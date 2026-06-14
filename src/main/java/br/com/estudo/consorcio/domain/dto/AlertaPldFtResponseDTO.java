package br.com.estudo.consorcio.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertaPldFtResponseDTO(
        Long lanceId,
        Long cotaId,
        String nomeConsorciado,
        String cpfCnpj,
        BigDecimal valorOferta,
        String tipoLance,
        LocalDateTime dataOferta,
        Long grupoId,
        String codigoGrupo
) {}
