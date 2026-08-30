package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.SimulacaoApuracaoResponseDTO;
import br.com.estudo.consorcio.service.AssembleiaAvancadaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssembleiaAvancadaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(username = "admin@consorcio.com.br", roles = {"ADMIN"})
class AssembleiaAvancadaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssembleiaAvancadaService service;

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
    @DisplayName("Deve obter simulação de apuração dry-run da assembleia")
    void deveObterSimulacao() throws Exception {
        SimulacaoApuracaoResponseDTO dto = new SimulacaoApuracaoResponseDTO(
                10L, 5L, new BigDecimal("100000.00"), 1, List.of(8),
                0, List.of(), new BigDecimal("50000.00"), "SHA256-ABCD1234"
        );

        when(service.simularApuracao(10L)).thenReturn(dto);

        mockMvc.perform(get("/api/assembleias-avancadas/assembleia/10/simular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assembleiaId").value(10))
                .andExpect(jsonPath("$.totalSorteadosPrevistos").value(1));
    }
}