package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoAmortizacaoLance;
import jakarta.validation.constraints.NotNull;

/** Request para a liquidação identificada de um lance vencedor. */
public record LiquidacaoLanceRequestDTO(@NotNull TipoAmortizacaoLance tipoAmortizacao) {
}
