package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import java.time.LocalDate;

public record AssembleiaResponseDTO(
        Long id,
        LocalDate dataAssembleia,
        TipoAssembleia tipo,
        Long grupoId
) {}