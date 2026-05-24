package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoLance;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LanceRequestDTO(
    @NotNull Long cotaId,
    @NotNull Long assembleiaId,
    @NotNull TipoLance tipo,
    @NotNull @Positive BigDecimal valorOferta
) {}
