package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.model.TipoContemplacao;
import br.com.estudo.consorcio.service.ContemplacaoService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContemplacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_GRUPOS", "VIEW_GRUPOS"})
class ContemplacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ContemplacaoService service;

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
    @DisplayName("Deve registrar contemplação com sucesso")
    void deveRegistrarContemplacao() throws Exception {
        ContemplacaoRequestDTO req = new ContemplacaoRequestDTO(
                10L, 1L, TipoContemplacao.SORTEIO, BigDecimal.ZERO, false
        );
        ContemplacaoResponseDTO resp = new ContemplacaoResponseDTO(
                1L, 10L, 1L, TipoContemplacao.SORTEIO, BigDecimal.ZERO, LocalDate.now(),
                false, new BigDecimal("80000.00"), "GRP-01", "João", "12345678901",
                "CONFIRMADA", 100, null
        );

        when(service.registrar(any(ContemplacaoRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/contemplacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipoContemplacao").value("SORTEIO"));
    }

    @Test
    @DisplayName("Deve listar contemplações por assembleia")
    void deveListarPorAssembleia() throws Exception {
        ContemplacaoResponseDTO resp = new ContemplacaoResponseDTO(
                1L, 10L, 5L, TipoContemplacao.LANCE_LIVRE, new BigDecimal("20000.00"), LocalDate.now(),
                false, new BigDecimal("60000.00"), "GRP-01", "João", "12345678901",
                "CONFIRMADA", 100, 15L
        );

        when(service.listarPorAssembleia(5L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/contemplacoes/assembleia/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assembleiaId").value(5));
    }

    @Test
    @DisplayName("Deve cancelar contemplação por atraso")
    void deveCancelarContemplacao() throws Exception {
        mockMvc.perform(post("/api/contemplacoes/lances/10/cancelar"))
                .andExpect(status().isOk());

        verify(service).cancelarContemplacaoPorAtraso(10L);
    }
}