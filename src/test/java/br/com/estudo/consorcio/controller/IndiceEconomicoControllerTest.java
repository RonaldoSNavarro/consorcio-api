package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.IndiceEconomicoDTO;
import br.com.estudo.consorcio.domain.dto.SimulacaoReajusteResponseDTO;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.service.BcbSgsService;
import br.com.estudo.consorcio.service.GrupoService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndiceEconomicoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_GRUPOS", "MANAGE_GRUPOS"})
class IndiceEconomicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BcbSgsService bcbService;

    @MockitoBean
    private GrupoService grupoService;

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
    @DisplayName("Deve obter histórico dos últimos 12 meses do índice BACEN")
    void deveObterUltimos12Meses() throws Exception {
        IndiceEconomicoDTO dto = new IndiceEconomicoDTO(IndiceReajuste.IPCA, LocalDate.now(), new BigDecimal("0.45"));
        when(bcbService.buscarAtualizarUltimos12Meses(IndiceReajuste.IPCA)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/indices-economicos/IPCA/ultimos-12-meses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoIndice").value("IPCA"));
    }

    @Test
    @DisplayName("Deve simular reajuste com sucesso")
    void deveSimularReajuste() throws Exception {
        SimulacaoReajusteResponseDTO sim = new SimulacaoReajusteResponseDTO(
                IndiceReajuste.INCC, new BigDecimal("4.50"), new BigDecimal("1.045"),
                new BigDecimal("100000.00"), new BigDecimal("104500.00"), List.of()
        );

        when(bcbService.simularReajuste(eq(IndiceReajuste.INCC), any(BigDecimal.class))).thenReturn(sim);

        mockMvc.perform(get("/api/indices-economicos/simular")
                .param("tipoIndice", "INCC")
                .param("valorAtual", "100000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novoValorCalculado").value(104500.00));
    }
}