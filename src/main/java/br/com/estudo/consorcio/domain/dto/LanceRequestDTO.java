package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoLance;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LanceRequestDTO(
    @NotNull Long cotaId,
    @NotNull Long assembleiaId,
    @NotNull TipoLance tipo,
    BigDecimal valorOferta,
    ModalidadeLance modalidade
) {}
