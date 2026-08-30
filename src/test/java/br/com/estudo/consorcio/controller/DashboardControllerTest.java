package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.KpiFinanceiroDTO;
import br.com.estudo.consorcio.service.DashboardService;
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

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_DASHBOARD", "VIEW_FINANCEIRO"})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService service;

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
    @DisplayName("Deve obter KPIs analíticos do dashboard")
    void deveObterKpis() throws Exception {
        KpiFinanceiroDTO kpi = new KpiFinanceiroDTO(
                new BigDecimal("500000.00"), new BigDecimal("1000000.00"), new BigDecimal("50.00"),
                new BigDecimal("2.50"), new BigDecimal("120000.00"), 15L,
                new BigDecimal("75000.00"), new BigDecimal("400000.00"), new BigDecimal("25000.00")
        );

        when(service.obterKpis()).thenReturn(kpi);

        mockMvc.perform(get("/api/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrecadacaoTotal").value(500000.00))
                .andExpect(jsonPath("$.totalContemplacoes").value(15));
    }
}