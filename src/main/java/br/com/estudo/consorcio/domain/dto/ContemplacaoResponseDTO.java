package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoContemplacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContemplacaoResponseDTO(
        Long id,
        Long cotaId,
        Long assembleiaId,
        TipoContemplacao tipoContemplacao,
        BigDecimal valorLance,
        LocalDate dataContemplacao,
        Boolean lanceEmbutido,
        BigDecimal valorCreditoLiberado,
        String codigoGrupo,
        String nomeCliente,
        String cpfCnpjCliente,
        String statusCota,
        Integer numeroCota
) {}