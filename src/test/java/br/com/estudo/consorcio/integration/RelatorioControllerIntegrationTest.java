package br.com.estudo.consorcio.integration;

import br.com.estudo.consorcio.domain.dto.AlertaPldFtResponseDTO;
import br.com.estudo.consorcio.domain.dto.BalanceteResponseDTO;
import br.com.estudo.consorcio.domain.dto.EstatisticasGrupoResponseDTO;
import br.com.estudo.consorcio.service.RelatorioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class RelatorioControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private RelatorioService relatorioService;

    @Test
    @WithMockUser(authorities = "VIEW_RELATORIOS")
    @DisplayName("Deve permitir acesso ao balancete para ADMIN")
    void devePermitirAdminAcessarBalancete() throws Exception {
        BalanceteResponseDTO mockResponse = new BalanceteResponseDTO(1L, "G-001", LocalDate.now(), List.of());
        when(relatorioService.gerarBalancete(eq(1L), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/relatorios/balancete/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoGrupo").value("G-001"));
    }

    @Test
    @WithMockUser(authorities = "VIEW_RELATORIOS")
    @DisplayName("Deve permitir acesso ao balancete para AUDITOR")
    void devePermitirAuditorAcessarBalancete() throws Exception {
        BalanceteResponseDTO mockResponse = new BalanceteResponseDTO(1L, "G-001", LocalDate.now(), List.of());
        when(relatorioService.gerarBalancete(eq(1L), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/relatorios/balancete/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONSORCIADO")
    @DisplayName("Deve negar acesso ao balancete para CONSORCIADO (Forbidden 403)")
    void deveNegarConsorciadoAcessarBalancete() throws Exception {
        mockMvc.perform(get("/api/relatorios/balancete/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve negar acesso ao balancete para usuário não autenticado (Forbidden 403)")
    void deveNegarAcessoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/relatorios/balancete/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "VIEW_RELATORIOS")
    @DisplayName("Deve permitir ADMIN acessar estatísticas e PLD/FT")
    void devePermitirAdminAcessarEstatisticasEPldFt() throws Exception {
        EstatisticasGrupoResponseDTO mockEst = new EstatisticasGrupoResponseDTO(
                1L, "G-001", LocalDate.now(), LocalDate.now(), 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO);
        when(relatorioService.gerarEstatisticas(eq(1L), any(), any())).thenReturn(mockEst);

        mockMvc.perform(get("/api/relatorios/estatisticas/1")
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoGrupo").value("G-001"));

        AlertaPldFtResponseDTO mockPld = new AlertaPldFtResponseDTO(
                1L, 1L, "Nome", "CPF", new BigDecimal("50000"), "LIVRE", LocalDateTime.now(), 1L, "G-001");
        when(relatorioService.gerarAlertaPldFt(any(), any())).thenReturn(List.of(mockPld));

        mockMvc.perform(get("/api/relatorios/pld-ft")
                        .param("dataInicio", "2026-01-01T00:00:00")
                        .param("dataFim", "2026-01-31T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeConsorciado").value("Nome"));
    }
}
