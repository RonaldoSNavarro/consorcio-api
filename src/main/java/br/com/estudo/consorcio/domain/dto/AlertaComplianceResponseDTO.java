package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Dados detalhados do alerta de compliance gerado")
public record AlertaComplianceResponseDTO(
        @Schema(example = "1054", description = "ID único do alerta")
        Long alertaId,

        @Schema(example = "89", description = "ID do cliente associado")
        Long clienteId,

        @Schema(example = "JOHN DOE", description = "Nome do cliente cadastrado")
        String nomeCliente,

        @Schema(example = "11122233344", description = "CPF/CNPJ do cliente cadastrado")
        String cpfCnpj,

        @Schema(example = "OFAC", description = "Origem da lista restritiva (PEP, OFAC, ONU)")
        OrigemListaRestritiva origemLista,

        @Schema(example = "JOHN DOE", description = "Nome completo encontrado na lista restritiva")
        String nomeEncontradoLista,

        @Schema(example = "1.00", description = "Score de similaridade obtido no matching (de 0.00 a 1.00)")
        java.math.BigDecimal scoreSimilaridade,

        @Schema(example = "PENDENTE_ANALISE", description = "Status atual de deliberação do alerta")
        StatusAlertaCompliance status,

        @Schema(description = "Data e hora de detecção do alerta")
        LocalDateTime dataDeteccao,

        @Schema(example = "Coincidência exata de CPF na lista da OFAC", description = "Justificativa ou histórico de deliberações do alerta")
        String justificativa
) {}
