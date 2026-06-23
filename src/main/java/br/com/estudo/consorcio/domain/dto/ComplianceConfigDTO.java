package br.com.estudo.consorcio.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Configuração do agendamento cron do módulo de Compliance")
public record ComplianceConfigDTO(
        @Schema(example = "0 0 3 * * *", description = "Expressão cron utilizada pelo agendador")
        String cronExpression,

        @Schema(example = "DIARIO", description = "Frequência de execução (DIARIO, SEMANAL, MENSAL)")
        String frequencia,

        @Schema(example = "03:00", description = "Horário de execução no formato HH:mm")
        String horario,

        @Schema(description = "Data e hora da última atualização da configuração")
        LocalDateTime dataAtualizacao
) {}
