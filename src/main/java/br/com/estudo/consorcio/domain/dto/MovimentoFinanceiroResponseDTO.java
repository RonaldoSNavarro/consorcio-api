package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.NaturezaMovimento;
import br.com.estudo.consorcio.domain.model.TipoMovimentoFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimentoFinanceiroResponseDTO(
        Long id,
        Long grupoId,
        Long cotaId,
        Long parcelaId,
        Long contemplacaoId,
        TipoMovimentoFinanceiro tipoMovimento,
        NaturezaMovimento natureza,
        BigDecimal valor,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        String descricao,
        LocalDateTime dataMovimento,
        LocalDate dataReferencia,
        String nomeUsuario
) {}
