package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusGrupo;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GrupoResponseDTO(
        Long id,
        String codigo,
        BigDecimal valorCredito,
        Integer prazoMeses,
        BigDecimal taxaAdministracao,
        StatusGrupo status,
        LocalDate dataCriacao,
        LocalDate dataInauguracao
) {}