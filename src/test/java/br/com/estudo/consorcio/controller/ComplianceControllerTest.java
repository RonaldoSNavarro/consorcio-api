package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ComplianceSyncResultDTO;
import br.com.estudo.consorcio.domain.mapper.ComplianceExecucaoLogMapper;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ComplianceConfigRepository;
import br.com.estudo.consorcio.repository.ComplianceExecucaoLogRepository;
import br.com.estudo.consorcio.service.ComplianceSincronizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplianceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_COMPLIANCE", "MANAGE_COMPLIANCE"})
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplianceSincronizacaoService sincronizacaoService;

    @MockitoBean
    private AlertaComplianceRepository alertaRepository;

    @MockitoBean
    private ComplianceConfigRepository configRepository;

    @MockitoBean
    private ComplianceExecucaoLogRepository execucaoLogRepository;

    @MockitoBean
    private ComplianceExecucaoLogMapper execucaoLogMapper;

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
    @DisplayName("Deve disparar sincronização manual de listas restritivas")
    void deveSincronizarListasManualmente() throws Exception {
        mockMvc.perform(post("/api/compliance/sincronizar"))
                .andExpect(status().isAccepted());

        verify(sincronizacaoService).sincronizarListas();
    }
}