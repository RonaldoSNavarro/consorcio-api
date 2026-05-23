package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusCota;

public record CotaResponseDTO(
        Long id,
        Integer numeroCota,
        Long clienteId,
        Long grupoId,
        StatusCota status,
        Integer versao
) {}