package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Detalhamento do alerta de PLD/FT baseado em valores elevados de lance")
public record AlertaPldFtResponseDTO(
        @Schema(example = "12", description = "ID único do lance")
        Long lanceId,

        @Schema(example = "5", description = "ID da cota ofertante")
        Long cotaId,

        @Schema(example = "JOÃO DA SILVA", description = "Nome do consorciado")
        String nomeConsorciado,

        @Schema(example = "11122233344", description = "CPF ou CNPJ do consorciado")
        String cpfCnpj,

        @Schema(example = "55000.00", description = "Valor ofertado no lance")
        BigDecimal valorOferta,

        @Schema(example = "LANCE_LIVRE", description = "Tipo do lance ofertado")
        String tipoLance,

        @Schema(description = "Data e hora em que a oferta foi realizada")
        LocalDateTime dataOferta,

        @Schema(example = "1", description = "ID do grupo")
        Long grupoId,

        @Schema(example = "GRP-AUTO-002", description = "Código identificador do grupo")
        String codigoGrupo
) {}
