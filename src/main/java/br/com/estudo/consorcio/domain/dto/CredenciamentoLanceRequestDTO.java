package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import br.com.estudo.consorcio.domain.model.TipoLance;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CredenciamentoLanceRequestDTO(
        @NotNull(message = "cotaId é obrigatório")
        Long cotaId,

        @NotNull(message = "assembleiaId é obrigatório")
        Long assembleiaId,

        @NotNull(message = "tipoLance é obrigatório")
        TipoLance tipoLance,

        @NotNull(message = "modalidade é obrigatória")
        ModalidadeLance modalidade,

        @NotNull(message = "valorLance é obrigatório")
        @Positive(message = "valorLance deve ser positivo")
        BigDecimal valorLance,

        BigDecimal valorFgts,
        BigDecimal valorEmbutido,
        BigDecimal valorProprio,
        Boolean termosAceitos
) {}