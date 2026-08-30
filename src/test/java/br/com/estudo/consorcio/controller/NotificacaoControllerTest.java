package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.NotificacaoDTO;
import br.com.estudo.consorcio.domain.enums.CanalNotificacao;
import br.com.estudo.consorcio.domain.enums.StatusNotificacao;
import br.com.estudo.consorcio.domain.enums.TipoNotificacao;
import br.com.estudo.consorcio.service.NotificacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_DASHBOARD", "VIEW_COTAS"})
class NotificacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacaoService service;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionService intrusionDetectionService;

    @MockitoBean
    private br.com.estudo.consorcio.config.SecurityFilter securityFilter;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private br.com.estudo.consorcio.service.TokenService tokenService;

    @MockitoBean
    private br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    @Test
    @DisplayName("Deve listar notificações não lidas por cliente")
    void deveListarNaoLidas() throws Exception {
        NotificacaoDTO dto = new NotificacaoDTO(
                1L, TipoNotificacao.CONTEMPLACAO, CanalNotificacao.INBOX, StatusNotificacao.ENVIADA,
                "Parabéns!", "Você foi contemplado", LocalDateTime.now(), null, 10L, 100L
        );

        when(service.listarNaoLidas(10L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notificacoes/cliente/10/nao-lidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Parabéns!"));
    }

    @Test
    @DisplayName("Deve marcar notificação como lida com sucesso")
    void deveMarcarComoLida() throws Exception {
        NotificacaoDTO dto = new NotificacaoDTO(
                1L, TipoNotificacao.CONTEMPLACAO, CanalNotificacao.INBOX, StatusNotificacao.LIDA,
                "Parabéns!", "Você foi contemplado", LocalDateTime.now(), LocalDateTime.now(), 10L, 100L
        );

        when(service.marcarComoLida(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/notificacoes/1/marcar-lida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIDA"));
    }
}