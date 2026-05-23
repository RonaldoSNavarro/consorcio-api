package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusCota;
import java.time.LocalDateTime;

public record HistoricoVersaoCotaResponseDTO(
        Long id,
        Long cotaId,
        Integer versao,
        StatusCota statusAnterior,
        StatusCota statusNovo,
        String motivo,
        LocalDateTime dataTransicao,
        String nomeUsuario
) {}
