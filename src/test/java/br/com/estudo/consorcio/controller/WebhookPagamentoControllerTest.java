package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.BoletoPixResponseDTO;
import br.com.estudo.consorcio.domain.dto.WebhookPixRequestDTO;
import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import br.com.estudo.consorcio.service.IntegracaoBancariaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookPagamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
class WebhookPagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private IntegracaoBancariaService service;

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
    @DisplayName("Deve processar webhook do banco com sucesso")
    void deveProcessarWebhookPix() throws Exception {
        WebhookPixRequestDTO req = new WebhookPixRequestDTO("TXID-123456", new BigDecimal("1250.00"), new BigDecimal("1.50"), "E2E-01");
        BoletoPixResponseDTO resp = new BoletoPixResponseDTO(
                1L, 10L, 50L, "23790.00109", "237919500", "TXID-123456", "PIX...",
                new BigDecimal("1250.00"), LocalDate.now(), StatusPagamentoBancario.PAGO
        );

        when(service.processarWebhookPix(any(WebhookPixRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/webhooks/pagamentos/pix")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }
}