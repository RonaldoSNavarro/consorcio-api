package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.service.ParcelaService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParcelaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
class ParcelaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ParcelaService service;

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

    @MockitoBean
    private br.com.estudo.consorcio.domain.repository.UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("CA-02: Deve registrar pagamento da parcela com Role ADMIN")
    void devePagarParcelaComRoleAdmin() throws Exception {
        LocalDate dataPag = LocalDate.now();
        ParcelaResponseDTO resp = new ParcelaResponseDTO(
                1L, 10L, 1, new BigDecimal("800.00"), new BigDecimal("0.80"),
                new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("20.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"),
                dataPag, dataPag, StatusParcela.PAGA
        );

        when(service.pagar(eq(1L), eq(dataPag))).thenReturn(resp);

        mockMvc.perform(put("/api/parcelas/1/pagar")
                .with(csrf())
                .param("dataPagamento", dataPag.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"));
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_FINANCEIRO"})
    @DisplayName("CA-04: Deve registrar pagamento da parcela com autoridade MANAGE_FINANCEIRO")
    void devePagarParcelaComAuthorityManageFinanceiro() throws Exception {
        LocalDate dataPag = LocalDate.now();
        ParcelaResponseDTO resp = new ParcelaResponseDTO(
                1L, 10L, 1, new BigDecimal("800.00"), new BigDecimal("0.80"),
                new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("20.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"),
                dataPag, dataPag, StatusParcela.PAGA
        );

        when(service.pagar(eq(1L), eq(dataPag))).thenReturn(resp);

        mockMvc.perform(put("/api/parcelas/1/pagar")
                .with(csrf())
                .param("dataPagamento", dataPag.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"));
    }

    @Test
    @WithMockUser(roles = {"CONSORCIADO"})
    @DisplayName("Deve negar pagamento de parcela para usuário sem perfil financeiro/admin")
    void deveNegarPagamentoSemPermissao() throws Exception {
        LocalDate dataPag = LocalDate.now();

        mockMvc.perform(put("/api/parcelas/1/pagar")
                .with(csrf())
                .param("dataPagamento", dataPag.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("CA-05: Deve estornar pagamento da parcela com Role ADMIN")
    void deveEstornarParcelaComCsrfValido() throws Exception {
        ParcelaResponseDTO resp = new ParcelaResponseDTO(
                1L, 10L, 1, new BigDecimal("800.00"), new BigDecimal("0.80"),
                new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("20.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), null, StatusParcela.PENDENTE
        );

        when(service.estornar(1L)).thenReturn(resp);

        mockMvc.perform(post("/api/parcelas/1/estornar")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Deve cadastrar parcela com Role ADMIN")
    void deveCadastrarParcelaComRoleAdmin() throws Exception {
        ParcelaRequestDTO req = new ParcelaRequestDTO(
                10L, 1, new BigDecimal("800.00"), new BigDecimal("150.00"),
                new BigDecimal("30.00"), new BigDecimal("20.00"), LocalDate.now().plusMonths(1)
        );
        ParcelaResponseDTO resp = new ParcelaResponseDTO(
                1L, 10L, 1, new BigDecimal("800.00"), new BigDecimal("0.80"),
                new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("20.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now().plusMonths(1), null, StatusParcela.PENDENTE
        );

        when(service.salvar(any(ParcelaRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/parcelas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @WithMockUser(roles = {"CONSORCIADO"})
    @DisplayName("Deve listar parcelas por cota com perfil CONSORCIADO")
    void deveListarPorCotaComPerfilConsorciado() throws Exception {
        ParcelaResponseDTO resp = new ParcelaResponseDTO(
                1L, 10L, 1, new BigDecimal("800.00"), new BigDecimal("0.80"),
                new BigDecimal("150.00"), new BigDecimal("30.00"), new BigDecimal("20.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), null, StatusParcela.PENDENTE
        );
        when(service.listarPorCota(10L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/parcelas/cota/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cotaId").value(10));
    }
}