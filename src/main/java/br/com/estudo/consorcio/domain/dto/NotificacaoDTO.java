package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.enums.CanalNotificacao;
import br.com.estudo.consorcio.domain.enums.StatusNotificacao;
import br.com.estudo.consorcio.domain.enums.TipoNotificacao;

import java.time.LocalDateTime;

public record NotificacaoDTO(
        Long id,
        TipoNotificacao tipo,
        CanalNotificacao canal,
        StatusNotificacao status,
        String titulo,
        String mensagem,
        LocalDateTime dataEnvio,
        LocalDateTime dataLeitura,
        Long clienteId,
        Long cotaId
) {}