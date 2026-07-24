package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.StatusGrupo;
import java.math.BigDecimal;
import java.time.LocalDate;
import br.com.estudo.consorcio.domain.enums.CategoriaBem;

public record GrupoResponseDTO(
        Long id,
        String codigoGrupo,
        BigDecimal valorCredito,
        Integer prazoMeses,
        BigDecimal taxaAdministracao,
        StatusGrupo status,
        LocalDate dataCriacao,
        LocalDate dataInauguracao,
        CategoriaBem categoriaBem,
        br.com.estudo.consorcio.domain.model.IndiceReajuste indiceReajuste,
        Integer mesReajuste,
        Integer quantidadeCotas,
        java.util.List<BemReferenciaResponseDTO> bensPermitidos,
        java.util.List<Integer> prazosPermitidos
) {}