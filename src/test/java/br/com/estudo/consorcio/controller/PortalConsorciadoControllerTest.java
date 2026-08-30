package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.PortalCotaDTO;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.service.PortalConsorciadoService;
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

@WebMvcTest(PortalConsorciadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(username = "12345678900", roles = {"CONSORCIADO"})
class PortalConsorciadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortalConsorciadoService service;

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
    @DisplayName("Deve listar cotas do consorciado autenticado")
    void deveListarMinhasCotas() throws Exception {
        PortalCotaDTO dto = new PortalCotaDTO(
                100L, "GRP-01", 20, StatusCota.ATIVA,
                new BigDecimal("90000.00"), new BigDecimal("45000.00"), 10, 60
        );

        when(service.listarCotasDoCliente("12345678900")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/portal/minhas-cotas?cpfCnpj=12345678900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cotaId").value(100))
                .andExpect(jsonPath("$[0].codigoGrupo").value("GRP-01"));
    }

    @Test
    @DisplayName("Deve rejeitar tentativa de IDOR quando consorciado comum tenta acessar CPF alheio")
    void deveRejeitarIdorQuandoConsorciadoTentaAcessarCpfAlheio() throws Exception {
        mockMvc.perform(get("/api/portal/minhas-cotas?cpfCnpj=99999999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Deve permitir que administrador consulte cotas de cliente específico")
    void devePermitirAdminConsultarCotasDeCliente() throws Exception {
        PortalCotaDTO dto = new PortalCotaDTO(
                100L, "GRP-01", 20, StatusCota.ATIVA,
                new BigDecimal("90000.00"), new BigDecimal("45000.00"), 10, 60
        );

        when(service.listarCotasDoCliente("99999999999")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/portal/minhas-cotas?cpfCnpj=99999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cotaId").value(100));
    }
}