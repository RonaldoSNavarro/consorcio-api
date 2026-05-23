package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.TipoInteracao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoConsorciadoResponseDTO(
        Long id,
        Long clienteId,
        Long cotaId,
        Long grupoId,
        TipoInteracao tipoInteracao,
        String descricao,
        
        // Snapshots
        BigDecimal valorCredito,
        BigDecimal valorFundoComum,
        BigDecimal valorFundoReserva,
        BigDecimal valorSeguro,
        BigDecimal valorCategoria,
        
        // Bem
        String descricaoBem,
        BigDecimal valorBem,
        
        // Parcela
        Long parcelaId,
        Integer numeroParcela,
        BigDecimal valorParcela,
        
        LocalDateTime dataInteracao,
        String nomeUsuario
) {}
