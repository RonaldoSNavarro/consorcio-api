package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BoletoPixResponseDTO(
        Long id,
        Long cotaId,
        Long parcelaId,
        String linhaDigitavel,
        String codigoBarras,
        String txid,
        String pixCopiaECola,
        BigDecimal valor,
        LocalDate dataVencimento,
        StatusPagamentoBancario status
) {}