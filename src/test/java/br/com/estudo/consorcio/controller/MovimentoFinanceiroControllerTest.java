package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.MovimentoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.model.NaturezaMovimento;
import br.com.estudo.consorcio.domain.model.TipoMovimentoFinanceiro;
import br.com.estudo.consorcio.service.MovimentoFinanceiroService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimentoFinanceiroController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_FINANCEIRO"})
class MovimentoFinanceiroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimentoFinanceiroService service;

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
    @DisplayName("Deve obter extrato financeiro do grupo")
    void deveListarPorGrupo() throws Exception {
        MovimentoFinanceiroResponseDTO dto = new MovimentoFinanceiroResponseDTO(
                1L, 10L, null, null, null,
                TipoMovimentoFinanceiro.PAGAMENTO_PARCELA, NaturezaMovimento.CREDITO,
                new BigDecimal("1000.00"), new BigDecimal("50000.00"), new BigDecimal("51000.00"),
                "Pagamento Parcela", LocalDateTime.now(), LocalDate.now(), "admin"
        );

        when(service.listarPorGrupo(10L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/grupos/10/movimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupoId").value(10));
    }

    @Test
    @DisplayName("Deve obter saldo do grupo")
    void deveObterSaldoGrupo() throws Exception {
        when(service.obterSaldoGrupo(10L)).thenReturn(new BigDecimal("125000.00"));

        mockMvc.perform(get("/api/grupos/10/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(125000.00));
    }
}