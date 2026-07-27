package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import br.com.estudo.consorcio.domain.model.StatusAssembleia;
import br.com.estudo.consorcio.domain.model.AlgoritmoPedraChave;
import java.time.LocalDate;

public record AssembleiaResponseDTO(
        Long id,
        LocalDate dataAssembleia,
        TipoAssembleia tipo,
        Long grupoId,
        StatusAssembleia status,
        Integer numeroSorteado,
        Integer premioExcluidos,
        String numeroExtracaoLoteria,
        AlgoritmoPedraChave algoritmoUsado,
        Integer pedraChaveCalculada,
        Integer fallbacksAplicados
) {}
