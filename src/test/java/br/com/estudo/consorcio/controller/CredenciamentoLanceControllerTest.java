package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import br.com.estudo.consorcio.domain.model.TipoLance;
import br.com.estudo.consorcio.service.CredenciamentoLanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CredenciamentoLanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(username = "12345678900", roles = {"CONSORCIADO"})
class CredenciamentoLanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CredenciamentoLanceService service;

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
    @DisplayName("Deve submeter credenciamento de lance com sucesso")
    void deveSubmeterCredenciamento() throws Exception {
        CredenciamentoLanceRequestDTO req = new CredenciamentoLanceRequestDTO(
                10L, 20L, TipoLance.FIRME, ModalidadeLance.LIVRE,
                new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15000.00"), true
        );

        CredenciamentoLanceResponseDTO resp = new CredenciamentoLanceResponseDTO(
                1L, 10L, 5, 20L, TipoLance.FIRME, ModalidadeLance.LIVRE,
                new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15000.00"),
                StatusCredenciamento.ATIVO, "HASH-123456", LocalDateTime.now()
        );

        when(service.credenciarLance(any(), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/credenciamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.hashAssinatura").value("HASH-123456"));
    }
}